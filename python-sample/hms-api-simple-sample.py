"""
file: hms-api-simple-sample.py
description: Sample python code for utilizing the HMS API.
Simplified for readability (no error handling or print statements)
date: 04-09-2019
"""
import requests
import json
import time


class HMS:
    """
    Python sample code for accessing the HMS web API. Functions as a CLI tool.
    If download is successful, data will be written to a json file.
    """
    request_url = "https://qed.epacdx.net/hms/rest/api/v3/"
    data_url = "https://qed.epacdx.net/hms/rest/api/v2/hms/data?job_id="
    swagger_url = "https://qed.epacdx.net/hms/api_doc/swagger/"

    request_body = {}
    task = None
    task_id = None
    result = None

    def __init__(self, component, dataset, source, start_date, end_date, geometry, timestep):
        """
        Initialization of the HMS request class object.
        :param component: HMS data component, valid values are 'hydrology', 'meteorology', and 'workflow'
        :param dataset: HMS component dataset, each component contains multiple datasets that can be found on the
        website or from the swagger api.
        :param source: Component dataset data source.
        :param start_date: Time series start date.
        :param end_date: Time series end date.
        :param geometry: Time series location, type and format depend on dataset source.
        :param timestep: Time series timestep, valid values are 'default', 'daily', 'weekly', 'monthly', 'yearly'
        """
        self.component = component
        self.dataset = dataset
        self.source = source
        self.start_date = start_date
        self.end_date = end_date
        self.geometry = geometry
        self.timestep = timestep
        self.setRequestBody()
        self.submitTask()
        self.getData()

    def setRequestBody(self):
        """
        Sets the input parameters to the request json object obtained from the hms swagger api documentation.
        """
        # GET request of the swagger api documentation
        swagger_json = json.loads(requests.get(self.swagger_url).text)
        # Request body example for the specified component and dataset
        self.request_body = swagger_json["paths"]["/api/" + self.component + "/" + self.dataset]["post"]["parameters"][0]["schema"]["example"]
        self.request_body["source"] = self.source
        self.request_body["dateTimeSpan"] = {
            "startDate": self.start_date,
            "endDate": self.end_date
        }
        self.request_body["geometry"] = self.geometry
        self.request_body["temporalResolution"] = self.timestep

    def submitTask(self):
        """
        Sends the request for data to the HMS API which triggers a new task.
        Request response returns a task ID and task status.
        """
        # POST request body
        params = json.dumps(self.request_body)
        # POST request to execute task
        self.task = json.loads(requests.post(url=self.request_url + self.component + "/" + self.dataset, data=params).text)
        self.task_id = self.task["job_id"]

    def getData(self):
        """
        Queries the HMS data endpoint for the tasks current status.
        """
        delay = 5
        time.sleep(delay)
        # GET request to query task status and data retrieval
        r1 = requests.get(url=self.data_url + self.task_id)
        self.result = json.loads(r1.text)
        if self.result["status"] == "SUCCESS":
            print("HMS task successful")
            self.saveData()
        elif self.result["status"] == "PENDING":
            print("Error: Unable to start task")
        elif self.result["status"] == "STARTED":
            self.getData()
        elif self.result["status"] == "FAILURE":
            print("Error: Task failed to complete")

    def saveData(self):
        """
        Writes the downloaded data to json file.
        """
        with open('hms-sample-data.json', 'w') as json_file:
            json.dump(self.result["data"], json_file)


def main():
    # API inputs
    cmp = "hydrology"
    ds = "precipitation"
    src = "nldas"
    date0 = "2010-01-01"
    date1 = "2010-12-31"
    geometry = {
        "point": {
            "latitude": 33.925,
            "longitude": -83.355
        }
    }
    ts = "daily"
    HMS(cmp, ds, src, date0, date1, geometry, ts)


if __name__ == "__main__":
    main()
