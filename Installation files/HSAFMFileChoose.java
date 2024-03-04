
import javax.swing.*;
import java.io.File;
import java.awt.BorderLayout;
import java.awt.event.*;
import java.io.*;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.frame.*;


import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.ByteOrder;





public class HSAFMFileChoose extends JFrame implements ActionListener{
    JLabel label;

    public void FileOpenMYG(){
        HSAFMFileChoose frame = new HSAFMFileChoose();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBounds(10, 10, 300, 200);
        frame.setTitle("HS-AFM File Open");
        frame.setVisible(true);
    }

    HSAFMFileChoose(){
        JButton button = new JButton("file select");
        button.addActionListener(this);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(button);

        label = new JLabel();

        JPanel labelPanel = new JPanel();
        labelPanel.add(label);

        getContentPane().add(labelPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.PAGE_END);
    }

    public void actionPerformed(ActionEvent e){
        JFileChooser filechooser = new JFileChooser();

        int selected = filechooser.showOpenDialog(this);
        if (selected == JFileChooser.APPROVE_OPTION){
            //File file = filechooser.getSelectedFile();
            File file = filechooser.getSelectedFile();



            InputStream is = null;
            DataInputStream dis = null;



            //	int[] intArray = new int[5];
	



            long nn;

            try {
                is = new FileInputStream(file.getAbsolutePath());
                dis = new DataInputStream(is);
	
                String str;
	
////////////Variables//////////////
ByteBuffer b1;
int FileFormatNumber=99;
int fileheader=0;
int frameheader=0;
int framenumber=0;
int XPixel=0;
int YPixel=0;
int XRange=0;
int YRange=0;
int CommentSize=0;
float ZPiezoC=0;
float ZPiezoDriverGain=0;
float ADRange = 0;
// ---END------Variables-----------


////////////Checking file format number//////////////

			nn = 0;
			dis.skip(nn);
                b1 = ByteBuffer.allocate(4);
                b1.putInt(dis.readInt());
                b1.order(ByteOrder.LITTLE_ENDIAN);
			FileFormatNumber = b1.getInt(0);
               	
//---END--------Checking file format number-------------



///////////////////////////////////////////////////////////////////
////////////READING PARAMETERS  (file format 1)////////////////////
	if(FileFormatNumber == 1){
                nn =0;
                dis.skip(nn);    
          
			b1 = ByteBuffer.allocate(4);
                b1.putInt(dis.readInt());
                b1.order(ByteOrder.LITTLE_ENDIAN);
                fileheader = b1.getInt(0);//////////// FILE HEADER
			

                b1 = ByteBuffer.allocate(4);
                b1.putInt(dis.readInt());
                b1.order(ByteOrder.LITTLE_ENDIAN);
                frameheader = b1.getInt(0);//////////FRAME HEADER


                nn =20;
                dis.skip(nn);

                b1 = ByteBuffer.allocate(4);
                b1.putInt(dis.readInt());
                b1.order(ByteOrder.LITTLE_ENDIAN);
                framenumber = b1.getInt(0);//////////FRAME Number


                nn =12;
                dis.skip(nn);

                b1 = ByteBuffer.allocate(4);
                b1.putInt(dis.readInt());
                b1.order(ByteOrder.LITTLE_ENDIAN);
                XPixel = b1.getInt(0);/////////////////X PIXEL

                b1 = ByteBuffer.allocate(4);
                b1.putInt(dis.readInt());
                b1.order(ByteOrder.LITTLE_ENDIAN);
                YPixel = b1.getInt(0);/////////////////Y PIXEL


                b1 = ByteBuffer.allocate(4);
                b1.putInt(dis.readInt());
                b1.order(ByteOrder.LITTLE_ENDIAN);
                XRange = b1.getInt(0);///////////////X RANGE

                b1 = ByteBuffer.allocate(4);
                b1.putInt(dis.readInt());
                b1.order(ByteOrder.LITTLE_ENDIAN);
                YRange = b1.getInt(0);///////////////Y RANGE


                nn =69;
                dis.skip(nn);

                b1 = ByteBuffer.allocate(4);
                b1.putInt(dis.readInt());
                b1.order(ByteOrder.LITTLE_ENDIAN);
                //ADRange = b1.getInt(0);/////////////// AD Range (-1 to 1, -2.5 to 2.5, -5 to 5 [V] )
			float temp;
			temp = b1.getInt(0);
			if(temp == 262144){
				ADRange = 10;
			}
			if(temp == 131072){
				ADRange = 5;
			}
			if(temp == 65536){
				ADRange = 2;
			}


                nn =20;  //  93
                dis.skip(nn);

                b1 = ByteBuffer.allocate(4);
                b1.putFloat(dis.readFloat());
                b1.order(ByteOrder.LITTLE_ENDIAN);
                ZPiezoC = b1.getFloat(0);///////////////Z Piezo Constant

                b1 = ByteBuffer.allocate(4);
                b1.putFloat(dis.readFloat());
                b1.order(ByteOrder.LITTLE_ENDIAN);
                ZPiezoDriverGain = b1.getFloat(0);///////////////Z Piezo Driver Gain



			//frameheader = frameheader + (XPixel * YPixel * 2);


		}

//---END------------READING PARAMETERS (file format 1)---------------------------------------



///////////////////////////////////////////////////////////////////
////////////READING PARAMETERS  (file format 0)////////////////////
	if(FileFormatNumber == 0){
                nn =4;
                dis.skip(nn);    
          
			b1 = ByteBuffer.allocate(4);
                b1.putInt(dis.readInt());
                b1.order(ByteOrder.LITTLE_ENDIAN);
                fileheader = b1.getInt(0);//////////// FILE HEADER


                b1 = ByteBuffer.allocate(4);
                b1.putInt(dis.readInt());
                b1.order(ByteOrder.LITTLE_ENDIAN);
                frameheader = b1.get(0);//////////FRAME HEADER


                nn =8;
                dis.skip(nn);

                b1 = ByteBuffer.allocate(4);
                b1.putInt(dis.readInt());
                b1.order(ByteOrder.LITTLE_ENDIAN);
                CommentSize = b1.getInt(0);/////////////////Comment Size


             //   nn =12;
             //  dis.skip(nn);

                b1 = ByteBuffer.allocate(4);
                b1.putShort(dis.readShort());
                b1.order(ByteOrder.LITTLE_ENDIAN);
                XPixel = b1.getShort(0);/////////////////X PIXEL

                b1 = ByteBuffer.allocate(4);
                b1.putShort(dis.readShort());
                b1.order(ByteOrder.LITTLE_ENDIAN);
                YPixel = b1.getShort(0);/////////////////Y PIXEL


                b1 = ByteBuffer.allocate(4);
                b1.putShort(dis.readShort());
                b1.order(ByteOrder.LITTLE_ENDIAN);
                XRange = b1.getShort(0);///////////////X RANGE

                b1 = ByteBuffer.allocate(4);
                b1.putShort(dis.readShort());
                b1.order(ByteOrder.LITTLE_ENDIAN);
                YRange = b1.getShort(0);///////////////Y RANGE


                nn =4;
                dis.skip(nn);

                b1 = ByteBuffer.allocate(4);
                b1.putFloat(dis.readFloat());
                b1.order(ByteOrder.LITTLE_ENDIAN);
                ZPiezoC = b1.getFloat(0);///////////////Z Piezo Constant

                b1 = ByteBuffer.allocate(4);
                b1.putFloat(dis.readFloat());
                b1.order(ByteOrder.LITTLE_ENDIAN);
                ZPiezoDriverGain = b1.getFloat(0);///////////////Z Piezo Driver Gain


                nn =43;
                dis.skip(nn);

                b1 = ByteBuffer.allocate(4);
                b1.putInt(dis.readInt());
                b1.order(ByteOrder.LITTLE_ENDIAN);
                framenumber = b1.getInt(0);//////////FRAME Number


			
			frameheader = frameheader;
			fileheader = fileheader +CommentSize + 88;  //// I do not understand what is the 88!

			ADRange = 10;


		}
//----END-----------READING PARAMETERS (file format 0)---------------------------------------




/////////////////////////////////////////////////////
//////////////DISPLAY///////////////////////////////
//	str = String.valueOf(FileFormatNumber);
//	IJ.showMessage("Format number",str);

//	str = String.valueOf(fileheader);
//	IJ.showMessage("file header",str);
	
//	str = String.valueOf(frameheader);
//	IJ.showMessage("frame header",str);

//	str = String.valueOf(framenumber);
//	IJ.showMessage("frame number",str);

//	str = String.valueOf(XPixel);
//	IJ.showMessage("X Pixel",str);

//	str = String.valueOf(YPixel);
//	IJ.showMessage("Y Pixel",str);

//	str = String.valueOf(XRange);
//	IJ.showMessage("X Range",str);

//	str = String.valueOf(YRange);
//	IJ.showMessage("Y Range",str);

//	str = String.valueOf(ADRange);
//	IJ.showMessage("ADRange",str);


//	str = String.valueOf(ZPiezoC);
//	IJ.showMessage("Z Piezo Constant",str);

//	str = String.valueOf(ZPiezoDriverGain);
//	IJ.showMessage("Z Piezo Driver Gain",str);


//	str = String.valueOf(CommentSize);
//	IJ.showMessage("Comment Size",str);





//-----END--------DISPLAY-----------------------------







////// ///////CLOSE FILE/////////////////////////////
                is.close();

//----END---------Close File---------------------------






//////////////////////////////////////////////////////////
////// ///////MAKING IMAGES/////////////////////////////
	
                float HeightAdjust = -1*ZPiezoC * ZPiezoDriverGain * ADRange/ 4096;
                //	str = String.valueOf(HeightAdjust);
                //	IJ.showMessage("Adjust",str);




 			IJ.run("Raw...", "open=[" + file.getAbsolutePath() +"] image=[16-bit Signed] width="+ String.valueOf(XPixel)+ " height=" + String.valueOf(YPixel) + " offset="+ String.valueOf( fileheader + frameheader) + " number="+ String.valueOf( framenumber) +" gap="+String.valueOf( frameheader) +" little-endian");
              
			ImagePlus imp1 = IJ.getImage();



                ImagePlus imp2 = IJ.createImage("HeightAdjust", "32-bit white", XPixel, YPixel, framenumber);
                IJ.run(imp2, "Add...", "value=" + String.valueOf(HeightAdjust) + " stack");			

//imp1.show();

                ImageCalculator ic = new ImageCalculator();
		
               ImagePlus imp3 = IJ.createImage("0", "32-bit white", XPixel, YPixel, framenumber);


//imp2.show();


		    imp3 = ic.run("Multiply create 32-bit stack", imp1, imp2);

	
                imp3.show();
 			imp1.close();

//int i;
//int f;
//double min, max;
//ImageStatistics stats;
//ImageProcessor ip = imp3.getProcessor();
//for(i=1;i<=framenumber;i++){

	
//	imp3.setSlice(i);		
//	min = ip.getMin();
IJ.run(imp3, "Measure", "Min");

	//IJ.run(imp3, "Add...", "value="+ String.valueOf(-1*min) + " slice");

	//IJ.run(imp3, "Next Slice [>]", "");


//	str = String.valueOf(min);
//	IJ.showMessage("min",str);
//}



//	str = String.valueOf(min);
//	IJ.showMessage("min",str);


                IJ.run( imp3,"Flip Vertically", "stack");
                IJ.run(imp3,"Orange Hot", "");
		
                // X Y (Z?) scale adjust
			float frx = XRange;
			float fry = YRange;
			float fpx = XPixel;
			float fpy = YPixel;
			float pixelsizeX = frx/fpx;
			float pixelsizeY = fry/fpy;
	


                IJ.run(imp3, "Properties...", "channels=1 slices=" + String.valueOf(framenumber)+" frames=1 unit=nm pixel_width=" + String.valueOf(pixelsizeX) +" pixel_height="+String.valueOf(pixelsizeY)+" voxel_depth=1 frame=[0 sec] origin=0,0");
          





//			IJ.run(imp3, "Enhance Contrast...", "saturated=0 process_all");


                 imp1.changes = false;
                

IJ.run("MYG LevelAdjuster", "");
IJ.resetMinAndMax(imp3);
			//imp2.close();
            }




            catch(IOException ee) {}



        }else if (selected == JFileChooser.CANCEL_OPTION){
            label.setText(" ");
        }else if (selected == JFileChooser.ERROR_OPTION){
            label.setText("error or canceled");
        }
    }

public static void MYGBeep(){
java.awt.Toolkit.getDefaultToolkit().beep();
}

}



//file.getAbsolutePath()
//
//slices=" + String.valueOf(framenumber)+
//java.awt.Toolkit.getDefaultToolkit().beep();/// beep sound
