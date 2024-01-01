import socket

SERVER = socket.gethostbyname(socket.gethostname()) 
PORT = 5050
ADDR = (SERVER, PORT)
FORMAT = 'utf-8'
DISCONNECT_MESSAGE = '!DISCONNECT'

def connect_to_server():
    client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    client.connect(ADDR)
    return client

def send_message(client, message):
    message += '\n'  # Append newline character to each message
    client.send(message.encode(FORMAT))
    print(client.recv(2048).decode(FORMAT))  # Print server response

client = connect_to_server()

send_message(client, "Hello Server!")
send_message(client, "How are you?")
send_message(client, DISCONNECT_MESSAGE)  # Send disconnect message

client.close()