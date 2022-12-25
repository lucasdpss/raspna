#!/usr/bin/env python
import pika

connection = pika.BlockingConnection(
    pika.ConnectionParameters(host='localhost'))
channel = connection.channel()

channel.queue_declare(queue='rpirelay', auto_delete=True, arguments={'x-message-ttl' : 2000})

channel.basic_publish(exchange='rpimessage', routing_key='rpirelay', body='on')
print(" [x] Sent 'on'")
connection.close()
