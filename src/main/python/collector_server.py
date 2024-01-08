import socket
import threading
from PIL import ImageGrab
import pygetwindow as gw
from datetime import datetime
import pickle
import gzip

def capture_screenshot():
    screenshot = ImageGrab.grab()
    return screenshot

def get_pixel_values(image):
    # Get the pixel values as a list of (R, G, B) tuples and grayscale values
    color_pixel_values = list(image.getdata())
    grayscale_pixel_values = list(image.convert('L').getdata())
    return color_pixel_values, grayscale_pixel_values


PORT = 5050
SERVER = socket.gethostbyname(socket.gethostname())
ADDR = (SERVER, PORT)
FORMAT = 'utf-8'
DISCONNECT_MESSAGE = '!DISCONNECT'

server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.bind(ADDR)



def handle_client(conn, addr):
    dataset = []
    print(f'[NEW CONNECTION] {addr} connected.')

    color_pixel_values, grayscale_pixel_values = (None, None)
    label = ('', 0)

    label_collected = False
    image_collected = False

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
        elif full_msg.strip() == 'Take Screenshot':
            print(f'[{addr}] {full_msg} gathering pixel data')
            screenshot = capture_screenshot()
            color_pixel_values, grayscale_pixel_values = get_pixel_values(screenshot)
            image_collected = True
        elif full_msg.strip().startswith('Block Recognition'):
            conn.send('Block recognition data stored\n'.encode(FORMAT))
            parts = full_msg.strip().split(' ')
            if len(parts) >= 4:
                print(f'[{addr}] {full_msg} recording label')
                block_state_string = parts[2]
                distance = parts[3]
                label = (block_state_string, distance)
                label_collected = True
        else:
            print(f'[{addr}] {full_msg}')
            conn.send('Msg received\n'.encode(FORMAT))
        if label_collected and image_collected:
            print('appending to dataset')
            dataset.append((color_pixel_values, grayscale_pixel_values, label))
            label_collected = False
            image_collected = False
    conn.close()
    store_dataset(dataset)
    print(f'dataset of length {len(dataset)} stored')
    
    
def store_dataset(dataset):
    # Serialize and save the dataset to a compressed pickle file
    with gzip.open('dataset.pkl.gz', 'wb') as file:
        pickle.dump(dataset, file)

def open_dataset():
    # Open the dataset from the compressed pickle file
    with gzip.open('dataset.pkl.gz', 'rb') as file:
        dataset = pickle.load(file)
    return dataset
        
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
