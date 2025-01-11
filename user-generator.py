import csv
import uuid
from faker import Faker

fake = Faker()

roles = ["SuperUser", "Moderator", "User"]

with open('random_users.csv', mode='w', newline='') as file:
    writer = csv.writer(file)
    writer.writerow(["UUID", "Name", "Email", "Role"])

    for _ in range(100):  # Generate 100 rows
        user_id = uuid.uuid4()
        name = fake.name()
        email = fake.email()
        role = fake.random_element(elements=roles)
        writer.writerow([user_id, name, email, role])