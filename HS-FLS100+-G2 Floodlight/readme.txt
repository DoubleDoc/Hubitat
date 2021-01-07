Tested 3 HomeSeer HS-FLS100+-G2 Floodlight Sensors as part of adapting this driver.

New to the Gen 2 (-G2) are sensitity control for the motion sensor, a temperature sensor, and significant changes to the control paramters.
The user manual explains these features, and is posted with the driver.

Following are notes from testing.

Temperature Sensor
Testing of all 3 units suggest internal heating is likely affecting the temperature sensor. At power on, each indicated reasonable measurements (withing 1 Deg.F).
Temperature reports increased gradually without the lights powered. Finally reaching about +5 degF above ambient of 68.5.
The device includes a parameter for calibration offsets of temperature. Testing indicated this parameter corresponds to Deg.F., same as the temperature sensor reports.
But, the manual incorrectly states the temperature calibration is Deg.C.

Light Level Sensor
In a low-light (subdued office) enclosed environment: two of three indicated 50-80 Lux readings. There was a large Lux report offset for one unit-9 and 10 Lux.
This affects local light activations for both motion and the dusk-dawn operation. 
The manual indicates a minimum setting of the control parameter threshold at 10 Lux for determinging dusk status.
So, one of the units may operate in some daytime environments such as complete cloud cover.
In a heavy cloud covered afternoon: 900 Lux reported. This is the maximum report per the manual.
So, the light sensor appears biased toward low-light situations typical of evenings/mornings and saturates at fair levels.

