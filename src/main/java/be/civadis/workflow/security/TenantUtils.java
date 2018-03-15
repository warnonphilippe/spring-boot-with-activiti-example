package be.civadis.workflow.security;

import java.util.Arrays;
import java.util.List;

public class TenantUtils {

    // pour le test avec une appli ne contenant qu'une DB H2 in mem, on utilise le schéma public
    // TODO : dans une application réelle, extraire le tenant courant de la secu et les tenants dispo de la config

    public static String getCurrentTenant(){
        return "public";
    }

    public static List<String> getTenants(){
        return Arrays.asList("public");
    }

}
