import sys
import uuid
from time import sleep

from confluent_kafka import Producer
import csv
import random

topics = ["api-v1", "api-v2"]
paths = ["media/movies", "media/audio", "media/images"]
http_types = [bytes([1]), bytes([2]), bytes([3]), bytes([4])]

# Kafka configuration
conf = {'bootstrap.servers': 'localhost:9098'}
producer = Producer(conf)

# Callback to confirm message delivery
def delivery_report(err, msg):
    if err is not None:
        print(f"Message delivery failed: {err}")
    else:
        print(f"Message delivered to {msg.topic()} [{msg.partition()}]")

if __name__ == "__main__":

    if len(sys.argv) != 2:
        print("Usage: python kafka_test.py <input_file>")
        sys.exit(1)

    users = []

    with open(sys.argv[1], mode='r') as file:
        reader = csv.reader(file)
        next(reader)  # Skip the header row
        for row in reader:
            users.append(row)
        for _ in range(100):
            user_uuid, name, email, role = random.choice(users)
            topic = random.choice(topics)

            user_id = uuid.UUID(user_uuid).bytes
            input_string = random.choice(paths)
            encoded_string = input_string.encode('utf-8')
            http_type = random.choice(http_types)

            result_bytes = user_id + http_type + encoded_string
            producer.produce(topic, result_bytes, callback=delivery_report)
            producer.flush()
            sleep(random.random())

    print("All messages sent!")
