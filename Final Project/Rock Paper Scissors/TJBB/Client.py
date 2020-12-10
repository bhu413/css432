import tkinter as tk
from tkinter import messagebox
import socket
import threading
from tkinter.constants import BOTH
import time

window = tk.Tk()
window.geometry("600x800")
window.title("Rock Paper Scissors")
window.configure(bg='#4c32a8')

window.grid_rowconfigure(0, weight=1)
window.grid_rowconfigure(5, weight=1)
window.grid_columnconfigure(0, weight=1)
window.grid_columnconfigure(5, weight=1)

def on_closing():
    if client != None:
        client.sendall("/unregister".encode())
    window.destroy()

window.protocol("WM_DELETE_WINDOW", on_closing)



chatFrame = tk.Frame(window, bg='#9752ff')
chatDisplay = tk.Text(chatFrame, height=40, width=40)
chatDisplay.grid(row = 0, column = 1, pady = 2)
chatInputBox = tk.Text(chatFrame, height=2, width=40)
chatInputBox.grid(row = 2, column = 1, pady = 2)
chatInputBox.config(highlightbackground="grey", state="disabled")
chatInputBox.bind("<Return>", (lambda event: get_chat_message(chatInputBox.get("1.0", tk.END))))

instructionFrame = tk.Frame(window, bg='#9752ff')
instructionLabel = tk.Label(instructionFrame, text="Commands:\n/sers - Lists users that are available to play")
instructionLabel.grid(row=1, column=3)

def connect():
    global client
    connect_to_server()
    chatFrame.grid(row=1, column=3)
    instructionFrame.grid(row=1, column=4, padx=10)



# network client
client = None
HOST_ADDR = "tjbb.bhu413.com"
HOST_PORT = 5000

def connect_to_server():
    global client, HOST_PORT, HOST_ADDR
    try:
        client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        client.connect((HOST_ADDR, HOST_PORT))

        chatInputBox.config(state=tk.NORMAL)

        # start a thread to keep receiving message from server
        # do not block the main thread :)
        threading._start_new_thread(receive_message_from_server, (client,))
    except Exception as e:
        tk.messagebox.showerror(title="ERROR!!!", message="Cannot connect to host: " + HOST_ADDR + " on port: " + str(HOST_PORT) + " Server may be Unavailable. Try again later\n" + str(e))


def receive_message_from_server(sck):
    while True:
        from_server = sck.recv(4096).decode("utf-8")

        if not from_server: break
        # display message from server on the chat window

        # enable the display area and insert the text and then disable.
        # why? Apparently, tkinter does not allow us insert into a disabled Text widget :(
        texts = chatDisplay.get("1.0", tk.END).strip()
        chatDisplay.config(state=tk.NORMAL)
        if len(texts) < 1:
            chatDisplay.insert(tk.END, from_server)
        else:
            chatDisplay.insert(tk.END, "\n\n"+ from_server)

        chatDisplay.config(state=tk.DISABLED)
        chatDisplay.see(tk.END)


    sck.close()
    window.destroy()


def get_chat_message(msg):

    msg = msg.replace('\n', '')
    texts = chatDisplay.get("1.0", tk.END).strip()

    send_mssage_to_server(msg)

    chatDisplay.see(tk.END)
    chatInputBox.delete('1.0', tk.END)


def send_mssage_to_server(msg):
    
    if msg == "exit":
        on_closing()
    else:
        client.sendall((msg + "\n").encode())
        print("Sending message")

connect()

window.mainloop()