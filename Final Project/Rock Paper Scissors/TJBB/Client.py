import tkinter as tk
from tkinter import messagebox
import socket
import threading
from tkinter.constants import BOTH
import time

window = tk.Tk()
window.geometry("800x500")
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


#btnConnect = tk.Button(window, text="Connect", command=lambda : connect())
#btnConnect.grid(row = 0, column = 0, pady = 2)

nameFrame = tk.Frame(window, bg='#4c32a8')

nameLabel = tk.Label(nameFrame, text="Please enter a username", bg='#4c32a8', fg='#ffffff')
nameLabel.grid(row=2, column=3, pady=10)
entName = tk.Entry(nameFrame)
entName.grid(row=3, column=3)
btnConnect = tk.Button(nameFrame, text="Connect", command=lambda : connect())
btnConnect.grid(row=4, column=3, pady=10)

nameFrame.grid(row=1, column=3)



chatFrame = tk.Frame(window, bg='#9752ff')
chatDisplay = tk.Text(chatFrame, height=20, width=30)
chatDisplay.grid(row = 0, column = 1, pady = 2)
chatInputBox = tk.Text(chatFrame, height=2, width=30)
chatInputBox.grid(row = 2, column = 1, pady = 2)
chatInputBox.config(highlightbackground="grey", state="disabled")
chatInputBox.bind("<Return>", (lambda event: get_chat_message(chatInputBox.get("1.0", tk.END))))

usersFrame = tk.Frame(window, bg='#9752ff')
btnUsers = tk.Button(chatFrame, text="Show Available Games", command=lambda : send_mssage_to_server("/users"))
btnUsers.grid(row=0, column=3)
userList = tk.Listbox(usersFrame, bg='#9752ff')


def challenged():
    print("challenged")

def updateUsers(users):
    usrArray = users.split("\n")
    for i in range(1, len(usrArray)):
        userList.insert(i, usrArray[i])
        print(usrArray[i])
    userList.grid(row=1, column=3)
    

def showInChatBox(message):
    # display message from server on the chat window

    # enable the display area and insert the text and then disable.
    # why? Apparently, tkinter does not allow us insert into a disabled Text widget :(
    texts = chatDisplay.get("1.0", tk.END).strip()
    chatDisplay.config(state=tk.NORMAL)
    if len(texts) < 1:
        chatDisplay.insert(tk.END, message)
    else:
        chatDisplay.insert(tk.END, "\n\n"+ message)

    chatDisplay.config(state=tk.DISABLED)
    chatDisplay.see(tk.END)



def connect():
    global client
    nameFrame.grid_forget()
    chatFrame.grid(row=1, column=0)
    usersFrame.grid(row=1,column=3)
    connect_to_server()
    send_mssage_to_server(entName.get())



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

        if "Current Users:" in from_server:
            updateUsers(from_server)

        else:
            showInChatBox(from_server)


    sck.close()
    window.destroy()


def get_chat_message(msg):

    msg = msg.replace('\n', '')
    texts = chatDisplay.get("1.0", tk.END).strip()

    # enable the display area and insert the text and then disable.
    # why? Apparently, tkinter does not allow use insert into a disabled Text widget :(
    chatDisplay.config(state=tk.NORMAL)
    if len(texts) < 1:
        chatDisplay.insert(tk.END, "You->" + msg, "tag_your_message") # no line
    else:
        chatDisplay.insert(tk.END, "\n\n" + "You->" + msg, "tag_your_message")

    chatDisplay.config(state=tk.DISABLED)

    send_mssage_to_server(msg)

    chatDisplay.see(tk.END)
    chatInputBox.delete('1.0', tk.END)


def send_mssage_to_server(msg):
    
    if msg == "exit":
        on_closing()
    else:
        client.sendall((msg + "\n").encode())
        print("Sending message")

window.mainloop()