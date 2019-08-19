from xml.etree import ElementTree as ET
import requests
import json
import time


class HMS:
	def __init__(self):
		self.loginUrl = "https://qed.epacdx.net/login/"
		self.etURL = "https://qed.epacdx.net/hms/rest/api/hydrology/evapotranspiration"#"https://qed.epacdx.net/hms/hydrology/evapotranspiration/"
		self.username = "qeduser"
		self.password = "ecoDomain2019"
		self.client = None
		self.evapoObject = {
			"source": "nldas",
			"algorithm": "penmandaily",
			"dateTimeSpan": {
				"startDate": "2010-01-01",
				"endDate": "2010-03-31"
			},
			"geometry": {
				"point":{
					"latitude": 33.925,
					"longitude": -83.355
				}
			},
			"albedo" : "0.23",
			"temporalResolution": "monthly"
		}

	def login(self):
		"""
		Logs into QED and starts a session.
		"""
		self.client = requests.session()
		i = requests.get(self.loginUrl)
		csrftoken = i.cookies["csrftoken"]
		header = {"Referer": self.loginUrl}
		request_data = {
			"username": self.username,
			"password": self.password,
			"csrfmiddlewaretoken": csrftoken,
			"next": "/"
		}
		data = self.client.post(self.loginUrl, data=request_data, headers=header, cookies=i.cookies)
		return data

	def make_request(self, url, post_data):
		"""
		Makes a POST request, returns dictionary of data.
		"""
		data = self.client.post(url, data=json.dumps(post_data), headers={'Content-Type': "application/json"})
		print(data.text)
		response_data = json.loads(data.text)
		return response_data

if __name__ == "__main__":
	api = HMS()

	print("Logging into QED.")
	login_result = api.login()

	print("Login result: {}".format(login_result.content))

	time.sleep(1)

	print("\nET Request.")
	newEvapoObject = dict(api.evapoObject)
	#print(api.etURL, newEvapoObject)
	response = api.make_request(api.etURL, newEvapoObject)
	print("{}".format(response))
	
	time.sleep(1)