package com.bonitasoft.custompage.foodtruck.generation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.codec.binary.Base64;

public class GenerateListingItem {

  public static String generateOneItem(final String path) {
    String xmlItem = "";

    final String[] fileNames = new String[] { "C:/git/bonita-internal-contrib/Custom_Pages/CraneTruck/resources/img/cranetruck.jpg",
        "E:/pym/Google Drive/developpement/customPage/custompage_scooter/custompage_scooter/resources/img/scooter.jpg",
        "E:/pym/Google Drive/developpement/customPage/custompage_american/custompage_american20150326/resources/img/american.jpg",
        "E:/pym/Google Drive/developpement/customPage/custompage_longboard/archive/custompage_longboard 20150409/resources/img/longboard.jpg",
        "E:/pym/Google Drive/developpement/customPage/custompage_meteor/custompage_meteor20150520/resources/img/meteor.jpg",
        "E:/pym/Google Drive/developpement/customPage/custompage_armoredtruck/img/armoredtruck.jpg",
        "E:/pym/Google Drive/developpement/customPage/custompage_awacs/custompage_awacs 20150903/awacs2.jpg",
        "C:/atelier/BPM-SP-7.1.0/workspace/tomcat/bonita/client/tenants/1/work/pages/custompage_containership/resources/img/containership2.jpg",
        "C:/atelier/BPM-SP-7.1.0/workspace/tomcat/bonita/client/tenants/1/work/pages/custompage_foodtruck/resources/img/foodtruck.jpg",
        "C:/atelier/BPM-SP-7.1.0/workspace/tomcat/bonita/client/tenants/1/work/pages/custompage_snowmobile/resources/img/snowmobile.jpg",
        "C:/atelier/BPM-SP-7.1.0/workspace/tomcat/bonita/client/tenants/1/work/pages/custompage_towtruck/resources/img/towtruck.jpg",
        "C:/atelier/BPM-SP-7.1.0/workspace/tomcat/bonita/client/tenants/1/work/pages/custompage_containership/resources/img/containership2.png" };

    for (final String fileName : fileNames) {
      FileInputStream fio;
      try {
        fio = new FileInputStream(new File(fileName));

        final java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
        int nRead;
        final byte[] data = new byte[16384];

        while ((nRead = fio.read(data, 0, data.length)) != -1) {
          os.write(data, 0, nRead);
        }

        final Base64 base64 = new Base64();
        xmlItem = "<logo>" + base64.encodeAsString(os.toByteArray()) + "</logo>";

        System.out.println("fileName=" + fileName + " = ");
        System.out.println("     " + xmlItem);

      } catch (final IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return xmlItem;
  }

  /**
   * @param args
   */
  public static void main(final String[] args) {
    // TODO Auto-generated method stub
    GenerateListingItem.generateOneItem("");
  }

}
