Router:
`java -jar Router <ATM port> <Bank Port>`

ATM:
`java -jar ATM <router IP> <router port>`

Bank:
`java -jar Bank <direct port> <router port> <router ip>`


This version temporarily disables the ATM connection for ease of testing.
In addition, this version requires use of a non-command-issuing telnet client. If you use PuTTY, be sure it is in passive mode.

This version changes the read statement. Now, the bank will start reading until it times out, or begins receiving data from the client.
Once it receives data, it will continue reading until it reads a newline or until data transmission is stopped.
This means that the bank cannot be frozen by an unresponsive client or a partially unresponsive client (who starts sending data and then stops).
However, the bank can be frozen by a client who constantly sends data with no newline and without stopping. Are we responsible for detecting and blocking this?
