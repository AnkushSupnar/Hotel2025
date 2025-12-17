/*
 * Old Quotation class for reference - kept for legacy code understanding
 * This class is deprecated - use KOTOrderPrint instead
 */
package com.frontend.print;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.sql.ResultSet;

import javax.print.PrintService;

// Old imports removed - use KOTOrderPrint for new implementation
// import ankush.CommonLogic;
// import ankush.CommonMethods;

import java.awt.FontMetrics;

/**
 *
 * @author mic
 */
public class Quotation extends javax.swing.JFrame
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	int table;
    /**
     * Creates new form bill_form
     */
    public Quotation(int table)
    {
    	this.table=table;
      // initComponents();
    	Print123();
    }
  
    public PageFormat getPageFormat(PrinterJob pj)
    {
    
    PageFormat pf = pj.defaultPage();
    Paper paper = pf.getPaper();    

    double middleHeight =8.0;  
    double headerHeight = 2.0;                  
    double footerHeight = 2.0;                  
    double width = convert_CM_To_PPI(8);      //printer know only point per inch.default value is 72ppi
    double height = convert_CM_To_PPI(headerHeight+middleHeight+footerHeight); 
    paper.setSize(width, height);
    paper.setImageableArea(                    
        0,
        10,
        width,            
        height - convert_CM_To_PPI(1)
    );   //define boarder size    after that print area width is about 180 points
            
    pf.setOrientation(PageFormat.PORTRAIT);           //select orientation portrait or landscape but for this time portrait
    pf.setPaper(paper);    

    return pf;
}
    
    protected static double convert_CM_To_PPI(double cm) {            
	        return toPPI(cm * 0.393600787);            
}
 
protected static double toPPI(double inch) {            
	        return inch * 72d;            
}






public class BillPrintable implements Printable 
{    
  @SuppressWarnings("unused")
public int print(Graphics graphics, PageFormat pageFormat,int pageIndex) 
  throws PrinterException 
  {    
 
      int result = NO_SUCH_PAGE;
      int tableno=table;
        if (pageIndex == 0) {                    
        
            Graphics2D g2d = (Graphics2D) graphics;                    

            double width = pageFormat.getImageableWidth();                    
           
            g2d.translate((int) pageFormat.getImageableX(),(int) pageFormat.getImageableY()); 

            ////////// code by alqama//////////////

            FontMetrics metrics=g2d.getFontMetrics(new Font("Shivaji",Font.BOLD,12));
        //    int idLength=metrics.stringWidth("000000");
            //int idLength=metrics.stringWidth("00");
            int idLength=metrics.stringWidth("000");
            int amtLength=metrics.stringWidth("000000");
            int qtyLength=metrics.stringWidth("00000");
            int priceLength=metrics.stringWidth("000000");
            int prodLength=(int)width - idLength - amtLength - qtyLength - priceLength-17;

        //    int idPosition=0;
        //    int productPosition=idPosition + idLength + 2;
        //    int pricePosition=productPosition + prodLength +10;
        //    int qtyPosition=pricePosition + priceLength + 2;
        //    int amtPosition=qtyPosition + qtyLength + 2;
            
            int productPosition = 0;
            int discountPosition= prodLength+5;
            int pricePosition = discountPosition +idLength+10;
            int qtyPosition=pricePosition + priceLength + 4;
            int amtPosition=qtyPosition + qtyLength;
            
            
              
        try{
            /*Draw Header*/
            int y=20;
            int yShift = 10;
            int headerRectHeight=15;
            int headerRectHeighta=40;
            
            ///////////////// Product names Get ///////////
                //String  pn1a=pn1.getText();
                String pn1a = "Name1";
                //String pn2a=pn2.getText();
                String pn2a = "Name2";
               // String pn3a=pn3.getText();
                String pn3a = "Name3";
                //String pn4a=pn4.getText();
                String pn4a = "Name4";
            ///////////////// Product names Get ///////////
      //getValues from TempTransaction
                String query = "select Itemname,printqty,Waitorid from TempTransaction where tableno="+table+" and Printqty>0";
 //String query = "select Itemname,printqty,rate,amt from TempTransaction where tableno="+tableno+" and Printqty>0";
                // Old code - deprecated
                ResultSet rs = null; // CommonMethods.selectQuery(query);
             
                	
                
            
            ///////////////// Product price Get ///////////
                //int pp1a=Integer.valueOf(pp1.getText());
                int pp1a=1;
                //int pp2a=Integer.valueOf(pp2.getText());
                int pp2a = 2;
                //int pp3a=Integer.valueOf(pp3.getText());
                int pp3a=3;
                //int pp4a=Integer.valueOf(pp4.getText());
                int pp4a=4;
                int sum=pp1a+pp2a+pp3a+pp4a;
            ///////////////// Product price Get ///////////
                int sr=1;
                String space=" ";
             g2d.setFont(new Font("Shivaji02",Font.PLAIN,12));
               // g2d.setFont(new Font("Shivaji01",Font.PLAIN,9));
         
            g2d.drawString("               ha^Tola AMjanaI  ",12,y);y+=yShift;
            g2d.drawString("                  Aa^Dr",12,y);y+=headerRectHeight;
            g2d.drawString("              tpiSala       naga        ",10,y);y+=yShift;
            g2d.setFont(new Font("Monospaced",Font.PLAIN,9));
            g2d.drawString("-----------------------",10,y);y+=headerRectHeight;
            int waitor = 0;
            g2d.setFont(new Font("Shivaji02",Font.PLAIN,12));
            while(rs.next())
            {
            	waitor=rs.getInt(3);
             g2d.drawString("           "+(sr++)+space+" "+Convertname(rs.getString(1))+"  "+rs.getInt(2)+"   ",10,y);y+=yShift;
            }
            
         
            if(sr>9)
            {
            	space="";
            }
            g2d.setFont(new Font("Monospaced",Font.PLAIN,12));
            g2d.drawString("--------------------------------",10,y);y+=headerRectHeight;
            g2d.setFont(new Font("Shivaji02",Font.PLAIN,12));
            g2d.drawString("             vaoTr -"+ waitor,10,y);y+=headerRectHeight; // CommonLogic.getWaitorName(waitor) - deprecated
            rs.close();
    }
        
    catch(Exception r){
    r.printStackTrace();
    }

              result = PAGE_EXISTS;    
          }    
          return result;    
      }
   }

public void Print123()
{
	@SuppressWarnings("unused")
	PrintService ps = findPrintService("Star BSC10 on ANJANISERVER-PC");
	
    PrinterJob pj = PrinterJob.getPrinterJob();
	
    pj.setPrintable(new BillPrintable(),getPageFormat(pj));
    try {
    	//pj.setPrintService(ps);
    	pj.print();
    		     
    }
     catch (PrinterException ex)
    {
             ex.printStackTrace();
    }	
}
public PrintService findPrintService(String printerName)
{
    for (PrintService service : PrinterJob.lookupPrintServices())
    {
        if (service.getName().equalsIgnoreCase(printerName))
            return service;
    }

    return null;
}

    
        public static void main(String args[])
         {
        	// CommonMethods.openConnection(); - deprecated
        	new Quotation(1);
        }
        String Convertname(String name)
        {
        	String Converted="";
        	for(int i=0;i<15;i++)
        	{
        		if(i< name.length())
        		{
        			Converted = Converted+name.charAt(i);
        		}
        		if(i>name.length())
        		{
        			Converted = Converted+" ";
        		}
        	}
        	return Converted;
        }
}