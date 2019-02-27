[![Project Status: Active – The project has reached a stable, usable state and is being actively developed.](http://www.repostatus.org/badges/latest/active.svg)](http://www.repostatus.org/#active)

# citygml2ucp

This program derives urban canopy parameters from CityGML data.


## Installation

* Install the [Java 11
  JDK](https://www.oracle.com/technetwork/java/javase/downloads/jdk11-downloads-5066655.html). Please
  ensure that `JAVA_PATH` points to the new installation.
* Download and install the [proj library](https://proj4.org/) (tested
  with version 5.2.0) with Java support. Usually, this requires manual
  compilation with something like

	```
	CFLAGS=-Iinclude2 ./configure --with-jni=include1
	```

  where
  * `include1` = folder in which the header file `jni.h` resides (usually `$JAVA_HOME/include`)
  * `include2` = folder in which the header file `jni_md.h` resides
    (usually `$JAVA_HOME/include/linux` or whatever)
  
  For convinience, the corresponding proj Java classes are included in
    the citygml2ucp source code.
* Run `mvn package` inside the citygml2ucp repository to build the jar file.
