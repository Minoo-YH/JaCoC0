public class TemperatureConverter {
    public double fahrenheitToCelsius(double fahrenheit) {
        return (fahrenheit - 32) * 5 / 9;
    }
    public double celsiusToFahrenheit(double celsius) {
        return (celsius * 9 / 5) + 32;
    }

    public boolean isExtremeTemperature(double celsius){

        return celsius < -40 || celsius > 50;
    }

    public double kelvinToCelsius(double kelvin) {
        return kelvin -273.15;
    }

    public static void main(String[] args) {
    // Sakhtane yek object az TemperatureConverter
    TemperatureConverter converter = new TemperatureConverter();

    // Tabdile 100 Celsius be Fahrenheit
    double result = converter.celsiusToFahrenheit(100);

    // Namayeshe natije
    System.out.println("100 Celsius = " + result + " Fahrenheit");
}

}
