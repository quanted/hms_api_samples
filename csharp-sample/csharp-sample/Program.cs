using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace csharp_sample
{
    class Program
    {
        static void Main(string[] args)
        {
            string component = "hydrology";
            string dataset = "precipitation";
            string source = "nldas";
            string startdate = "2010-01-01";
            string enddate = "2010-12-31";
            Dictionary<string, string> geometry = new Dictionary<string, string>();
            geometry.Add("latitude", "33.325");
            geometry.Add("longitude", "-83.525");
            string timestep = "daily";

            HMSSample hms = new HMSSample(component, dataset, source, startdate, enddate, geometry, timestep);
            hms.submitRequest();
            hms.getData();
            hms.writeToFile();

        }
    }
}
