import socket
import threading
from PIL import ImageGrab
import pygetwindow as gw
from datetime import datetime

def take_screenshot(filename):
    screenshot = ImageGrab.grab()
    screenshot.save(filename)


PORT = 5050
SERVER = socket.gethostbyname(socket.gethostname())
ADDR = (SERVER, PORT)
FORMAT = 'utf-8'
DISCONNECT_MESSAGE = '!DISCONNECT'

server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.bind(ADDR)

def handle_client(conn, addr):
    print(f'[NEW CONNECTION] {addr} connected.')

    connected = True
    while connected:
        full_msg = ''
        while True:
            msg = conn.recv(1024).decode(FORMAT)
            if not msg:
                break
            full_msg += msg
            if full_msg.endswith('\n'):
                break

        if full_msg.strip() == DISCONNECT_MESSAGE:
            connected = False
            print(f'[{addr}] {full_msg}')
            conn.send('Msg received\n'.encode(FORMAT))
        else:
            message_handler(full_msg.strip(), conn, addr)

    conn.close()
    
def message_handler(msg, conn, addr):
    if msg == 'Take Screenshot':
        print(f'[{addr}] {msg}')
        conn.send('screenshot taken\n'.encode(FORMAT))
        take_screenshot(f'data/{get_time()}.png')
    elif msg.startswith('Block Recognition'):
        print(f'[{addr}] {msg}')
        conn.send('Block recognition data stored\n'.encode(FORMAT))
        store_recognition_data(msg)
    else:
        print(f'[{addr}] {msg}')
        conn.send('Msg received\n'.encode(FORMAT))
    

def store_recognition_data(msg):
    pass
        
def get_time():
    # Get the current time
    current_time = datetime.now()
    # Format the time as a string
    time_string = current_time.strftime("%Y_%m_%d_%H_%M_%S_%f")[:19] # time down to the millisecond
    return time_string

def start():
    server.listen()
    print(f'[LISTENING] Server is listening on {SERVER}')
    while True:
        conn, addr = server.accept()
        thread = threading.Thread(target=handle_client, args=(conn, addr))
        thread.start()
        print(f'[ACTIVE CONNECTIONS] {threading.active_count() - 1}')

print('[STARTING] server is starting...')
start()
