"""Coze reverse-search node. Preserve car_number/now_location -> message list."""
import os
import mysql.connector
from runtime import Args
from typings.Reverse_car_search.Reverse_car_search import Input, Output


def handler(args: Args[Input]) -> Output:
    # The existing workflow's camera-location data source is kept configurable.
    # now_location is retained in the node schema, but is not a route origin yet.
    db = mysql.connector.connect(
        host=os.environ["DB_HOST"],
        port=int(os.environ.get("DB_PORT", "3306")),
        user=os.environ["DB_USER"],
        password=os.environ["DB_PASSWORD"],
        database=os.environ["DB_NAME"],
        connect_timeout=8,
    )
    cursor = db.cursor()
    try:
        cursor.execute(
            "SELECT camera_location FROM parking_car WHERE numberplate = %s",
            (args.input.car_number,),
        )
        return {"message": [str(row[0]) for row in cursor.fetchall() if row[0] is not None]}
    finally:
        cursor.close()
        db.close()
