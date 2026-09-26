# Temperature Converter - Code Coverage Assignment

This project is part of my in-class assignment about unit testing and code coverage in Java.

The goal was to create a simple temperature converter, write unit tests using JUnit 5, check the code coverage with JaCoCo, and run the project in Jenkins.

## What I implemented

The `TemperatureConverter` class has four methods:

- `fahrenheitToCelsius()` converts Fahrenheit to Celsius.
- `celsiusToFahrenheit()` converts Celsius to Fahrenheit.
- `kelvinToCelsius()` converts Kelvin to Celsius.
- `isExtremeTemperature()` checks if a Celsius temperature is below -40°C or above 50°C.

For Kelvin to Celsius, I used:

C = K - 273.15

For example, 300 K is 26.85°C.

## Testing

I used JUnit 5 to test the temperature conversion methods and the extreme temperature check.

All 4 tests passed successfully.

I created `TemperatureConverterTest` using JUnit 5.

I tested the conversion methods with different values, for example:

- 32°F = 0°C
- 212°F = 100°C
- 0°C = 32°F
- 100°C = 212°F

I also tested the extreme temperature method around the boundary values such as -40°C and 50°C.

This helped me understand why edge cases are important when writing unit tests.

## Code Coverage

I used JaCoCo with Maven to generate the code coverage report.

I ran the tests using:

```bash
mvn clean test
```

The JaCoCo HTML report is generated in:

```text
target/site/jacoco/index.html
```

The report shows line coverage, branch coverage, method coverage, and class coverage.

![Coverage Report](Coverage_report.png)

## Test Result

Here is the screenshot of my test results:

![Test Results](test.png)

## Jenkins

I created a Jenkins Freestyle project called `Minoo_Temperature_V1`.

Jenkins gets the project from GitHub and runs the Maven tests with:

```text
clean verify
```

The Jenkins build and tests completed successfully.

![Jenkins Test Result](Jenkin_test_result.png)

## Jenkins Pipeline

For the next part of the assignment, I created a Jenkins Pipeline for the same Temperature Converter project.

The pipeline includes these stages:

- Build
- Test
- Code Coverage
- Publish Test Results
- Publish Coverage Results
- Docker Build
- Docker Push

The final pipeline completed successfully.

![Jenkins Pipeline](image.png)

## Docker

I created a Dockerfile for the project and built a Docker image.

The Docker image is:

```text
minooyh/tempconverter:latest
```

The image was also pushed to Docker Hub from the Jenkins pipeline.

![Docker Hub](dockerhublatest.png)

## Running the Docker Image

I pulled the image from Docker Hub using:

```bash
docker pull minooyh/tempconverter:latest
```

Then I ran the image using:

```bash
docker run --rm minooyh/tempconverter:latest
```

The output was:

```text
100 Celsius = 212.0 Fahrenheit
```

![Docker Run](4.png)

## What I learned

From these assignments, I learned how to write unit tests with JUnit 5 and how to use `assertEquals`, `assertTrue`, and `assertFalse`.

I also learned why testing boundary values is important. For example, -40°C and 50°C are not extreme temperatures in my program, but values below -40°C or above 50°C are.

I learned how to use JaCoCo with Maven to check which parts of my code are covered by my tests.

I also learned how to create a Jenkins pipeline using a Jenkinsfile, build a Docker image, run the image, and push it to Docker Hub.
