import RPi.GPIO as GPIO
import subprocess

green_led_port = 21

GPIO.setwarnings(False)
GPIO.setmode(GPIO.BCM)
GPIO.setup(green_led_port, GPIO.OUT)

cmd = "ping -c 1 www.google.com"
res = (subprocess.call(cmd, shell=True))
if res == 0:
    GPIO.output(green_led_port, True)
else:
    GPIO.output(green_led_port, False)


