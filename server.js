const express = require("express");
const socket = require("socket.io");
const https = require("https");
const fs = require("fs");

const HOST = "192.168.0.45";
const PORT = 8765;
const app = express();

const server = https.createServer({
    cert: fs.readFileSync("./cert.pem"),
    key: fs.readFileSync("./key.key"),
    requestCert: true,
    ca: [
        fs.readFileSync('client/X509Certificate.crt')
    ]
}, app);

server.listen(PORT, function () {
    console.log(`Listening on port ${PORT}`);
    console.log(`https://${HOST}:${PORT}`);
});

// Static files
// app.use(express.static("public"));

const io = socket(server);

var socket_counter = 0;

io.on("connection", function (socket) {
    process.stdout.write(`Made socket connection from ${socket.handshake.address}...\t`);
    if (socket_counter == 2) {
        console.log(" but it was disconnected.")
        socket.emit("message", "There are already 2 users connected.");
        socket.disconnect();
        return;
    }
    process.stdout.write(" and the connection was established.\n")
    
    socket.on('disconnect', function() {
      console.log(`Got disconnect from ${socket.handshake.address}!`);
      socket_counter--;
      io.sockets.emit("message", "They left.");
   });
    
    socket.on("transfer", function(data) {
        socket.broadcast.emit("transfer", data);
        console.log(data)
        console.log(data.public_exponent)
        console.log(data.modulus)
    });

    socket_counter++;
    socket.emit("message", `Hello ${socket.handshake.address}!`)
    socket.broadcast.emit("message", "They connected.");
});


io.on("error", (err) => {
    if (err.message === "unauthorized event") {
        console.log("Made socket connection");
    }
});
