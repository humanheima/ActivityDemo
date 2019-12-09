Message next() {
    //...
    int nextPollTimeoutMillis = 0;
    for (;;) {
        if (nextPollTimeoutMillis != 0) {
            Binder.flushPendingCommands();
        }
        //感觉就是在这里阻塞的
        nativePollOnce(ptr, nextPollTimeoutMillis);

        synchronized (this) {
            //尝试获取下一个message。如果找到了，则返回。
            final long now = SystemClock.uptimeMillis();
            Message prevMsg = null;
            Message msg = mMessages;
            if (msg != null && msg.target == null) {
                // 存在同步屏障，查找队列中的下一个异步消息。
                do {
                    prevMsg = msg;
                    msg = msg.next;
                } while (msg != null && !msg.isAsynchronous());
            }
            if (msg != null) {
                if (now < msg.when) {
                    // 下一个消息还没准备好，设置超时在消息准备好时唤醒。
                    nextPollTimeoutMillis = (int) Math.min(msg.when - now, Integer.MAX_VALUE);
                } else {
                    // 获取了一个消息
                    mBlocked = false;
                    if (prevMsg != null) {
                        prevMsg.next = msg.next;
                    } else {
                        mMessages = msg.next;
                    }
                    msg.next = null;
                    if (DEBUG) Log.v(TAG, "Returning message: " + msg);
                    msg.markInUse();
                    //返回获取的消息
                    return msg;
                }
            } else {
                // No more messages.
                nextPollTimeoutMillis = -1;
            }

            // 现在处理退出消息已处理所有待处理消息。
            if (mQuitting) {
                dispose();
                return null;
            }

        }
           
        // 所以回去重新查找待处理的消息而无需等待。
        nextPollTimeoutMillis = 0;
    }
}
