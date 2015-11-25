# SimpleNioClientServer

This is a simple single threaded Java NIO client and server from 2008.
It is written in the style of ultra low latency trading systems of that era.

# How realistic is this code?

I worked for Island ECN/Inet ATS/Instinet from 2000-2008.
This was the company featured in [Dark Pools](http://www.amazon.com/gp/product/0307887189?keywords=dark%20pools&qid=1448422629&ref_=sr_1_1&sr=8-1) and this code is
similar to what we were writing at that time.

I wrote this code as a take home assignment from [Orc Software](http://www.orc-group.com/).
I got the job and worked at Orc from 2008-2009.

I am posting it *now* because a friend asked, and because it is a good, 
if overly simple, example of what high speed low latency Java trading 
software looked like at the time.

Also, because people have no idea what I mean when I say "Single threaded non-blocking code"
Now, I would just say that it works like the event loop in node.js
