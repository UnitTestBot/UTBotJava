import datetime


def get_data_labels(dates):
    if not dates:
        dates.append(datetime.time(hour=23, minute=59))
        return None
    if all(x.hour == 0 and x.minute == 0 for x in dates):
        return [x.strftime('%Y-%m-%d') for x in dates]
    else:
        return [x.strftime('%H:%M') for x in dates]
