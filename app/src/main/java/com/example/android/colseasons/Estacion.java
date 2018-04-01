package com.example.android.colseasons;

/**
 * Created by jdpp95 on 6/12/2017.
 */

public class Estacion extends Ciudad {

    String nEstacion;
    double temperatura, osD, osN, altitud;

    public Estacion(String nEstacion, String nombre, String departamento, double temperatura, double osD, double osN, double altitud, double latitud, double longitud)
    {
        super(nombre,departamento,latitud,longitud);
        this.nEstacion = nEstacion;
        this.temperatura = temperatura;
        this.osN = osN;
        this.osD = osD;
        this.altitud = altitud;
    }

    public String getnEstacion()
    {
        return nEstacion;
    }

    public double getTemperatura()
    {
        return temperatura;
    }

    public double getOsD()
    {
        return osD;
    }

    public double getOsN()
    {
        return osN;
    }

    public double getAltitud()
    {
        return altitud;
    }

    public String toString()
    {
        return nEstacion + ", " + nombre + ", " + departamento;
    }
}
