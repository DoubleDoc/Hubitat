Tested 3 HomeSeer HS-FLS100+-G2 Floodlight Sensors as part of adapting this driver.

New to the Gen 2 (-G2) are sensitity control for the motion sensor, a temperature sensor, and significant changes to the control paramters.
The user manual explains these features, and is posted with the driver.

Following are notes from testing.

Temperature Sensor
Testing of all 3 units suggest internal heating is likely affecting the temperature sensor. At power on, each indicated reasonable measurements (withing 1 Deg.F).
Temperature reports increased gradually without the lights powered. Finally reaching about +5 degF above ambient of 68.5.
Temperature reported is strongly affected by sunlight exposure. Morning sunlight increased the reported temperature 5 degF above ambient.
The device includes a parameter for calibration offsets of temperature. Testing indicated this parameter corresponds to Deg.F., same as the temperature sensor reports.
But, the manual incorrectly states the temperature calibration is Deg.C.

Light Level Sensor
In a low-light (subdued office) enclosed environment: two of three indicated 50-80 lux readings. There was a large lux report offset for one unit-9 and 10 lux.
This affects local light activations for both motion and the dusk-dawn operation. 
The manual indicates a minimum setting of the control parameter threshold at 10 lux for determinging dusk status.
In a heavy cloud covered afternoon: 900 lux reported. This is the maximum report per the manual. Night readings in dark are zero lux.
So, the light sensor appears biased toward low-light situations typical of evenings/mornings and saturates at fair levels.

Motion Sensor
Testing using the 'Low'(6 meter) sensitivity:
Mounted high-about 12 ft up over vegetation. Vehicle and people sensing at 2 times that distance. Few false motion events. Testing at night, 30-40 Deg.F. and no wind.
Mounted at 8 ft up and over a concrete drive then (12 ft) tall vegetation. A few unexplained motion events. Testing at night, 30-40 Deg.F. and no wind.
Mounted at 7 ft up over bed 3ft bed with adjacent (2 ft range) tall and narrow conifer in side view, 10 ft concrete drive, then lawn and taller vegetation. Many false alerts. Testing at night, 30-40 Deg.F. and almost no wind.
Testing using the 'High' (20 meter) sensitivity with the same sensors and setups:
Mounted high-about 12 ft up over vegetation. Reliable vehicle sensing at distance, and no false reports.  Testing at night into morning, 25-40 Deg.F. and no wind.
Mounted at 8 ft up and over a concrete drive then (12 ft) tall vegetation. Detected squirrel crossing drive. Single false event. Testing at night into morning, 25-40 Deg.F. and no wind.
--pending


