from datetime import datetime

print('Press CTRL + C to exit')
try:
    while True:
        try:
            t = int(input('Enter timestamp: '))
        except ValueError:
            print('Invalid timestamp')
            continue
        print(datetime.fromtimestamp(t).strftime("%A, %B %d, %Y %H:%M:%S"))
except KeyboardInterrupt:
    pass
