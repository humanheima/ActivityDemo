```java
private int startActivityUnchecked(final ActivityRecord r, ActivityRecord sourceRecord,
                                   IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
                                   int startFlags, boolean doResume, ActivityOptions options, TaskRecord inTask,
                                   ActivityRecord[] outActivity) {

    setInitialState(r, options, inTask, doResume, startFlags, sourceRecord, voiceSession,
            voiceInteractor);

    computeLaunchingTaskFlags();

    computeSourceStack();

    mIntent.setFlags(mLaunchFlags);

    //TODO 注释1处，查找可重用的Activity
    ActivityRecord reusedActivity = getReusableIntentActivity();

    final int preferredLaunchStackId =
            (mOptions != null) ? mOptions.getLaunchStackId() : INVALID_STACK_ID;
    final int preferredLaunchDisplayId =
            (mOptions != null) ? mOptions.getLaunchDisplayId() : DEFAULT_DISPLAY;

    //TODO 注释2处，有可重用的Activity
    if (reusedActivity != null) {
        // When the flags NEW_TASK and CLEAR_TASK are set, then the task gets reused but
        // still needs to be a lock task mode violation since the task gets cleared out and
        // the device would otherwise leave the locked task.
        if (mSupervisor.isLockTaskModeViolation(reusedActivity.getTask(),
                (mLaunchFlags & (FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK))
                        == (FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK))) {
            mSupervisor.showLockTaskToast();
            Slog.e(TAG, "startActivityUnchecked: Attempt to violate Lock Task Mode");
            return START_RETURN_LOCK_TASK_MODE_VIOLATION;
        }

        if (mStartActivity.getTask() == null) {
            mStartActivity.setTask(reusedActivity.getTask());
        }
        if (reusedActivity.getTask().intent == null) {
            // This task was started because of movement of the activity based on affinity...
            // Now that we are actually launching it, we can assign the base intent.
            reusedActivity.getTask().setIntent(mStartActivity);
        }

        // This code path leads to delivering a new intent, we want to make sure we schedule it
        // as the first operation, in case the activity will be resumed as a result of later
        // operations.
        if ((mLaunchFlags & FLAG_ACTIVITY_CLEAR_TOP) != 0
                || isDocumentLaunchesIntoExisting(mLaunchFlags)
                || mLaunchSingleInstance || mLaunchSingleTask) {
            final TaskRecord task = reusedActivity.getTask();

            // In this situation we want to remove all activities from the task up to the one
            // being started. In most cases this means we are resetting the task to its initial
            // state.

            //TODO 注释3处，如何设置了clearTop标记位，清除当前Activity上面的所有Activity，返回的top就是要重用的Activity
            final ActivityRecord top = task.performClearTaskForReuseLocked(mStartActivity,
                    mLaunchFlags);

            // The above code can remove {@code reusedActivity} from the task, leading to the
            // the {@code ActivityRecord} removing its reference to the {@code TaskRecord}. The
            // task reference is needed in the call below to
            // {@link setTargetStackAndMoveToFrontIfNeeded}.
            if (reusedActivity.getTask() == null) {
                reusedActivity.setTask(task);
            }

            if (top != null) {
                if (top.frontOfTask) {
                    // Activity aliases may mean we use different intents for the top activity,
                    // so make sure the task now has the identity of the new intent.
                    top.getTask().setIntent(mStartActivity);
                }
                //TODO 注释4处，返回的top就是要重用的，调用deliverNewIntent，
                // 如果Activity，不是处于resume状态，调用完 onNewIntent ，还会调用 onResume
                deliverNewIntent(top);
            }
        }

        sendPowerHintForLaunchStartIfNeeded(false /* forceSend */, reusedActivity);

        reusedActivity = setTargetStackAndMoveToFrontIfNeeded(reusedActivity);

        final ActivityRecord outResult =
                outActivity != null && outActivity.length > 0 ? outActivity[0] : null;

        // When there is a reused activity and the current result is a trampoline activity,
        // set the reused activity as the result.
        if (outResult != null && (outResult.finishing || outResult.noDisplay)) {
            outActivity[0] = reusedActivity;
        }

        if ((mStartFlags & START_FLAG_ONLY_IF_NEEDED) != 0) {
            // We don't need to start a new activity, and the client said not to do anything
            // if that is the case, so this is it!  And for paranoia, make sure we have
            // correctly resumed the top activity.
            resumeTargetStackIfNeeded();
            return START_RETURN_INTENT_TO_CALLER;
        }
        setTaskFromIntentActivity(reusedActivity);

        if (!mAddingToTask && mReuseTask == null) {
            // We didn't do anything...  but it was needed (a.k.a., client don't use that
            // intent!)  And for paranoia, make sure we have correctly resumed the top activity.
            resumeTargetStackIfNeeded();
            if (outActivity != null && outActivity.length > 0) {
                outActivity[0] = reusedActivity;
            }

            return START_TASK_TO_FRONT;
        }
    }

    if (mStartActivity.packageName == null) {
        final ActivityStack sourceStack = mStartActivity.resultTo != null
                ? mStartActivity.resultTo.getStack() : null;
        if (sourceStack != null) {
            sourceStack.sendActivityResultLocked(-1 /* callingUid */, mStartActivity.resultTo,
                    mStartActivity.resultWho, mStartActivity.requestCode, RESULT_CANCELED,
                    null /* data */);
        }
        ActivityOptions.abort(mOptions);
        return START_CLASS_NOT_FOUND;
    }

    // If the activity being launched is the same as the one currently at the top, then
    // we need to check if it should only be launched once.
    //TODO 注释5处，要启动的activity 就是当前栈顶的activity
    final ActivityStack topStack = mSupervisor.mFocusedStack;
    final ActivityRecord topFocused = topStack.topActivity();
    //TODO 栈顶的Activity
    final ActivityRecord top = topStack.topRunningNonDelayedActivityLocked(mNotTop);
    //TODO 注释6处，判断是否需要重新启动
    final boolean dontStart = top != null && mStartActivity.resultTo == null
            && top.realActivity.equals(mStartActivity.realActivity)
            && top.userId == mStartActivity.userId
            && top.app != null && top.app.thread != null
            && ((mLaunchFlags & FLAG_ACTIVITY_SINGLE_TOP) != 0
            || mLaunchSingleTop || mLaunchSingleTask);
    if (dontStart) {
        //不需要重新启动
        // For paranoia, make sure we have correctly resumed the top activity.
        topStack.mLastPausedActivity = null;
        if (mDoResume) {
            mSupervisor.resumeFocusedStackTopActivityLocked();
        }
        ActivityOptions.abort(mOptions);
        if ((mStartFlags & START_FLAG_ONLY_IF_NEEDED) != 0) {
            // We don't need to start a new activity, and the client said not to do
            // anything if that is the case, so this is it!
            return START_RETURN_INTENT_TO_CALLER;
        }
        //TODO 注释7处，调用Activity的 onNewIntent 方法。
        deliverNewIntent(top);

        // Don't use mStartActivity.task to show the toast. We're not starting a new activity
        // but reusing 'top'. Fields in mStartActivity may not be fully initialized.
        mSupervisor.handleNonResizableTaskIfNeeded(top.getTask(), preferredLaunchStackId,
                preferredLaunchDisplayId, topStack.mStackId);

        return START_DELIVERED_TO_TOP;
    }

    boolean newTask = false;
    final TaskRecord taskToAffiliate = (mLaunchTaskBehind && mSourceRecord != null)
            ? mSourceRecord.getTask() : null;

    // Should this be considered a new task?
    int result = START_SUCCESS;
    if (mStartActivity.resultTo == null && mInTask == null && !mAddingToTask
            && (mLaunchFlags & FLAG_ACTIVITY_NEW_TASK) != 0) {
        newTask = true;
        result = setTaskFromReuseOrCreateNewTask(
                taskToAffiliate, preferredLaunchStackId, topStack);
    } else if (mSourceRecord != null) {
        result = setTaskFromSourceRecord();
    } else if (mInTask != null) {
        result = setTaskFromInTask();
    } else {
        // This not being started from an existing activity, and not part of a new task...
        // just put it in the top task, though these days this case should never happen.
        setTaskToCurrentTopOrCreateNewTask();
    }
    if (result != START_SUCCESS) {
        return result;
    }

    mService.grantUriPermissionFromIntentLocked(mCallingUid, mStartActivity.packageName,
            mIntent, mStartActivity.getUriPermissionsLocked(), mStartActivity.userId);
    mService.grantEphemeralAccessLocked(mStartActivity.userId, mIntent,
            mStartActivity.appInfo.uid, UserHandle.getAppId(mCallingUid));
    if (mSourceRecord != null) {
        mStartActivity.getTask().setTaskToReturnTo(mSourceRecord);
    }
    if (newTask) {
        EventLog.writeEvent(
                EventLogTags.AM_CREATE_TASK, mStartActivity.userId,
                mStartActivity.getTask().taskId);
    }
    ActivityStack.logStartActivity(
            EventLogTags.AM_CREATE_ACTIVITY, mStartActivity, mStartActivity.getTask());
    mTargetStack.mLastPausedActivity = null;

    sendPowerHintForLaunchStartIfNeeded(false /* forceSend */, mStartActivity);

    mTargetStack.startActivityLocked(mStartActivity, topFocused, newTask, mKeepCurTransition,
            mOptions);
    if (mDoResume) {
        final ActivityRecord topTaskActivity =
                mStartActivity.getTask().topRunningActivityLocked();
        if (!mTargetStack.isFocusable()
                || (topTaskActivity != null && topTaskActivity.mTaskOverlay
                && mStartActivity != topTaskActivity)) {
            // If the activity is not focusable, we can't resume it, but still would like to
            // make sure it becomes visible as it starts (this will also trigger entry
            // animation). An example of this are PIP activities.
            // Also, we don't want to resume activities in a task that currently has an overlay
            // as the starting activity just needs to be in the visible paused state until the
            // over is removed.
            mTargetStack.ensureActivitiesVisibleLocked(null, 0, !PRESERVE_WINDOWS);
            // Go ahead and tell window manager to execute app transition for this activity
            // since the app transition will not be triggered through the resume channel.
            mWindowManager.executeAppTransition();
        } else {
            // If the target stack was not previously focusable (previous top running activity
            // on that stack was not visible) then any prior calls to move the stack to the
            // will not update the focused stack.  If starting the new activity now allows the
            // task stack to be focusable, then ensure that we now update the focused stack
            // accordingly.
            if (mTargetStack.isFocusable() && !mSupervisor.isFocusedStack(mTargetStack)) {
                mTargetStack.moveToFront("startActivityUnchecked");
            }
            //todo 注释8处，到这里，就得创建新的Activity了
            mSupervisor.resumeFocusedStackTopActivityLocked(mTargetStack, mStartActivity,
                    mOptions);
        }
    } else {
        mTargetStack.addRecentActivityLocked(mStartActivity);
    }
    mSupervisor.updateUserStackLocked(mStartActivity.userId, mTargetStack);

    mSupervisor.handleNonResizableTaskIfNeeded(mStartActivity.getTask(), preferredLaunchStackId,
            preferredLaunchDisplayId, mTargetStack.mStackId);

    return START_SUCCESS;
}
```

TODO 注释1处，查找可重用的Activity。
TODO 注释2处，有可重用的Activity。
TODO 注释3处，如何设置了clearTop标记位，清除当前Activity上面的所有Activity，返回的top就是要重用的Activity。
TODO 注释4处，返回的top就是要重用的，调用deliverNewIntent。如果Activity，不是处于resume状态，调用完 onNewIntent ,还会调用 onResume。
TODO 注释5处，要启动的activity 就是当前栈顶的activity
TODO 注释6处，判断是否需要重新启动
TODO 注释7处，调用Activity的 onNewIntent 方法。
TODO 注释8处，到这里，就得创建新的Activity了


