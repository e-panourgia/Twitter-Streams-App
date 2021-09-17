from flask import Flask,jsonify,request
from flask import render_template
import ast

app = Flask(__name__)

labels = []
values = []


@app.route("/")
def chart():
    global labels,values
    labels = []
    values = []
    return render_template('chart.html', values=values, labels=labels)


@app.route('/refreshData')
def refresh_graph_data():
    global labels, values
    print("labels now: " + str(labels))
    print("data now: " + str(values))
    return jsonify(sLabel=labels, sData=values)


@app.route('/updateData', methods=['POST'])
def update_data_post():
    global labels, values
    print (request.json)
    if not request.json:
        return "error",400
    labels = request.json['label']
    values = request.json['data']
    print("labels received: " + str(labels))
    print("data received: " + str(values))
    return "success",201


if __name__ == "__main__":
    app.run(host='localhost', port=5001)

