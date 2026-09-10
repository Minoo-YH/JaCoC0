import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;



public class TemperatureConverterTest {
   TemperatureConverter converter = new TemperatureConverter();
    @Test
    void testFahrenheitToCelsius(){


        assertEquals(0.0, converter.fahrenheitToCelsius(32), 0.001);
       assertEquals(100.0, converter.fahrenheitToCelsius(212), 0.001);
        assertEquals(-40.0, converter.fahrenheitToCelsius(-40), 0.001);


    }




    @Test
    void testCelsiusToFahrenheit(){
        assertEquals(32, converter.celsiusToFahrenheit(0), 0.001);
        assertEquals(212, converter.celsiusToFahrenheit(100), 0.001);
    }

    @Test
    void testIsExtremeTemperature(){

        assertTrue(converter.isExtremeTemperature(-50));
        assertTrue(converter.isExtremeTemperature(-41));
        assertFalse(converter.isExtremeTemperature(-40));
        assertFalse(converter.isExtremeTemperature(50));
        assertTrue(converter.isExtremeTemperature(51));

    }


}


