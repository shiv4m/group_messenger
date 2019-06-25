# group_messenger
An android based application that guarantees total & FIFO ordering of messages. The total guarantee is ensured by implementing ISIS algorithm which is a voting based protocol and thus can even work with node failures. An external thread is spawned and PING-ACK is used to detect a node failure.
