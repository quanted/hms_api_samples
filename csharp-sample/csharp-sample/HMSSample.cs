using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Net;
using System.IO;
using Newtonsoft.Json;

namespace csharp_sample
{
    /// <summary>
    /// HMS request date time span object
    /// </summary>
    public class DateTimeSpan
    {
        public string startDate;
        public string endDate;
    }

    /// <summary>
    /// HMS request point geometry object
    /// </summary>
    public class Point
    {
        public double latitude;
        public double longitude;
    }

    /// <summary>
    /// HMS request geometry object
    /// </summary>
    public class Geometry
    {
        public int comID;
        public string stationID;
        public Point point;
    }

    /// <summary>
    /// HMS request body object, necessary for json serialization.
    /// </summary>
    public class Request
    {
        public string source;
        public DateTimeSpan dateTimeSpan;
        public Geometry geometry;
        public string temporalResolution;
    }

    /// <summary>
    /// Sample code for using the HMS web API
    /// </summary>
    public class HMSSample
    {

        private string requestURL = "https://qed.epacdx.net/hms/rest/api/v3/";
        private string dataURL = "https://qed.epacdx.net/hms/rest/api/v2/hms/data?job_id=";

        private Request requestBody = null;
        private string component = null;
        private string dataset = null;

        private string taskID = null;
        private bool completed = false;
        private string result = null;
   
        /// <summary>
        /// Initialize the POST request object
        /// </summary>
        /// <param name="component">Component name: valid values include 'hydrology', 'meteorology', and 'workflow'</param>
        /// <param name="dataset">Dataset name: valid values are dependent on the specified component</param>
        /// <param name="source">Source name: valid values are dependent on the specified dataset</param>
        /// <param name="startDate">Time series start date as a string</param>
        /// <param name="endDate">Time series end date as a string</param>
        /// <param name="geometry">Dictionary of the geometry, key must be one of the attributes of the Geometry class</param>
        /// <param name="timestep">Time series timestep: valid values include 'default', 'daily', 'weekly', 'monthly', and 'yearly'</param>
        public HMSSample(string component, string dataset, string source, string startDate, string endDate, Dictionary<string, string> geometry, string timestep)
        {
            Console.WriteLine("HMS Web API Sample Tool");
            Console.WriteLine("Initializing request...");
            this.requestBody = new Request();
            this.requestBody.source = source;
            this.requestBody.dateTimeSpan = new DateTimeSpan();
            this.requestBody.dateTimeSpan.startDate = startDate;
            this.requestBody.dateTimeSpan.endDate = endDate;
            this.component = component;
            this.dataset = dataset;

            this.requestBody.geometry = new Geometry();
            if (geometry.ContainsKey("latitude"))
            {
                this.requestBody.geometry.point = new Point();
                this.requestBody.geometry.point.latitude = Double.Parse(geometry["latitude"]);
                this.requestBody.geometry.point.longitude =  Double.Parse(geometry["longitude"]);
            }
            else if (geometry.ContainsKey("stationID"))
            {
                this.requestBody.geometry.stationID = geometry["stationID"];
            }
            else if (geometry.ContainsKey("hucID"))
            {
                this.requestBody.geometry.comID = int.Parse(geometry["hucID"]);
            }
            else
            {
                Console.WriteLine("Error: No valid geometry value found.");
            }
            this.requestBody.temporalResolution = timestep;
            Console.WriteLine("Initialized POST request with inputs");
        }

        /// <summary>
        /// Submit POST request to HMS web API
        /// </summary>
        public void submitRequest()
        {
            Console.WriteLine("Submitting request to hms...");
            var request = (HttpWebRequest)WebRequest.Create(this.requestURL + "" + this.component + "/" + this.dataset + "/");
            var data = Encoding.ASCII.GetBytes(JsonConvert.SerializeObject(this.requestBody));
            request.Method = "POST";
            request.ContentType = "application/json";
            request.ContentLength = data.Length;
            using (var stream = request.GetRequestStream())
            {
                stream.Write(data, 0, data.Length);
            }
            var response = (HttpWebResponse)request.GetResponse();
            this.taskID = JsonConvert.DeserializeObject<Dictionary<string, string>>(new StreamReader(response.GetResponseStream()).ReadToEnd())["job_id"];
            Console.WriteLine("Submission completed.");
        }

        /// <summary>
        /// Start polling HMS data api, with a 5 second delay.
        /// </summary>
        public void getData()
        {
            this.completed = false;
            while (!completed)
            {
                int delay = 5000;
                Thread.Sleep(delay);
                var request = (HttpWebRequest)WebRequest.Create(this.dataURL + this.taskID);
                var response = (HttpWebResponse)request.GetResponse();
                var responseString = new StreamReader(response.GetResponseStream()).ReadToEnd();
                var data = JsonConvert.DeserializeObject<Dictionary<string, object>>(responseString);
                if (data["status"].Equals("SUCCESS"))
                {
                    completed = true;
                    this.result = data["data"] + Environment.NewLine;
                    Console.WriteLine("Data successfully downloaded");
                }
                else if (data["status"].Equals("FAILURE"))
                {
                    completed = true;
                    Console.WriteLine("Error: Failed to complete task.");
                }
            }
        }

        /// <summary>
        /// Writes the downloaded data to a json file.
        /// </summary>
        public void writeToFile()
        {
            string fileName = "hms-data.json";
            File.WriteAllText(fileName, this.result);
            Console.WriteLine("Completed writing data to file: " + fileName);
        }
    }
}
