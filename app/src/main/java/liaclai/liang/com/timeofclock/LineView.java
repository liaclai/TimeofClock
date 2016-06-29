package liaclai.liang.com.timeofclock;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


/**
 * 
 * 
 * 
 * 绘制图像的步骤：
 * 1.从文件中读取数据,计算后共有2048个数值
 * 2.最大值最为图线的3/4高度，最小值最为最低点,相邻的点相连
 * 
 * 缩放图像的功能:
 * 1.放大
 * 2.缩小
 * 3.移动
 * 
 */

@SuppressLint({ "NewApi", "DrawAllocation" })
public class LineView extends View {
	private static Context mContext;
	
	private static double TOTAL_WIDTH = 0;
	private static double TOTAL_HEIGHT = 0;
	private static double BLOCK_WIDTH = 0;
	private static double BLOCK_HEIGHT = 0;
	private final static int BLOCK_COUNT = 2048;	//2048个点
	private static double TABLE_WIDTH = 0;	
	private static double TABLE_HEIGHT = 0;	

	private double vWidth = 0;	//视图的宽度
	private double vHeight = 0;	//视图的高度
	
	private double [][] arr_2048_pix = new double[2048][2];     //2048个点
	private List<Point> points_temp = new ArrayList<Point>();	//临时坐标点集合

	private double scale = 1;	//当前的放大倍数
	private static final int scale_max = 10;	//scale的最大值
	private static final int scale_min = 1;		//scale的最小值
	
	
	/** 模式 NONE：无. MOVE：移动. ZOOM:缩放*/
	 
	private static final int NONE = 0;  
	private static final int MOVE = 1;  
	private static final int ZOOM = 2;  
	private int mode = NONE;	// 默认模式
	private double beforeLength = 0, afterLength = 0;	// 两触点距离 
	private double maxHDistance = 0;	        		//纵轴的最大高度

	private double downX = 0;	//单触点x坐标
	private double downY = 0;	//单触点y坐标
	
	private int tempIndex = 0;	//最高点坐标
	
	
	
	
	
	@SuppressWarnings("deprecation")
	public LineView(Context context){
		super(context);		
		mContext = context;	
		WindowManager winManager=(WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
		TOTAL_WIDTH = winManager.getDefaultDisplay().getWidth();	//屏幕宽度
		TOTAL_HEIGHT = winManager.getDefaultDisplay().getHeight();	//屏幕高度
		BLOCK_WIDTH = TOTAL_WIDTH*1.0/2200;		//每块的宽度(暂时修改)
		BLOCK_HEIGHT = TOTAL_HEIGHT*1.0/140;	//每块的高度
		TABLE_WIDTH = BLOCK_COUNT*BLOCK_WIDTH;	//表格的宽度
		TABLE_HEIGHT = BLOCK_HEIGHT*40;			//表格的高度	
		setvWidth(TABLE_WIDTH);
		setvHeight(TABLE_HEIGHT+1);
	}

	/**
	 * 绘制图像的方法,调用 invalidate()会自动调用该方法
	 */
	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
		//创建一个与该View相同大小的缓存区
		Bitmap cacheBitmap = Bitmap.createBitmap((int)getvWidth(), (int)getvHeight(), Config.ARGB_8888);
		Canvas cacheCanvas = new Canvas();
		//设置cacheCanvas将会绘制到内存中的cacheBitmap上  
		cacheCanvas.setBitmap(cacheBitmap);
		Paint paint = new Paint();
		paint.setStyle(Style.STROKE);
		paint.setAntiAlias(true);
		//绘制表格
		drawTable(cacheCanvas,paint);	
		//绘制图像
		drawAutoView(cacheCanvas,paint);
		
		Paint bmpPaint = new Paint();  
		//将cacheBitmap绘制到该View组件上  
		canvas.drawBitmap(cacheBitmap, 0, 0, bmpPaint); 
	}
	
	/**
	 * 绘制表
	 * @param cacheCanvas
	 */
	public void drawTable(Canvas cacheCanvas,Paint paint){

		Rect rectTable = new Rect(0,0,
				(int)(TABLE_WIDTH),(int) (TABLE_HEIGHT));

		paint.setColor(0xff625562);
		
		cacheCanvas.drawRect(rectTable, paint);

		for(int i=1;i<4;i++){
			cacheCanvas.drawLine((float)(i*TABLE_WIDTH*1.0/4), (float)0, (float)(i*TABLE_WIDTH*1.0/4), (float)(TABLE_HEIGHT), paint);		
		}

		for(int i=1;i<4;i++){
			cacheCanvas.drawLine((float)0, (float)(i*TABLE_HEIGHT*1.0/4), (float)(TABLE_WIDTH), (float)(i*TABLE_HEIGHT*1.0/4), paint);			
		}

		paint.setStrokeWidth(5);
		//坐标轴线
		cacheCanvas.drawLine((float)(0), (float)(TABLE_HEIGHT), (float)(TABLE_WIDTH*1.0), (float)(TABLE_HEIGHT), paint);
		cacheCanvas.drawLine((float)0, (float)0, (float)(0), (float)(TABLE_HEIGHT*1.0), paint);

	}



	/**
	 * 在画布上自动绘制图线
	 * @param cacheCanvas
	 */
	private void drawAutoView(Canvas cacheCanvas,Paint paint) {

		if(points_temp.size()==0) return;
		paint.setColor(0xff14878e);	
		paint.setStrokeWidth(2);
		paint.setTextSize(20);

		double max = maxHDistance;
		double min = 0;
		double diff = max-min;

		for(int i=0;i<points_temp.size();i++){	
			arr_2048_pix[i][0] = TABLE_WIDTH*1.0*points_temp.get(i).getX()/points_temp.size();// 屏幕上的x坐标
			arr_2048_pix[i][1] = TABLE_HEIGHT -3.0/4*TABLE_HEIGHT*points_temp.get(i).getY()/diff;// 屏幕上的y坐标

			//相邻两点相连
			if(i>0&&i<=2047){
				cacheCanvas.drawLine((float)arr_2048_pix[i-1][0],
						(float)arr_2048_pix[i-1][1],
						(float)arr_2048_pix[i][0],
						(float)arr_2048_pix[i][1], 
						paint);
			}
		}

		paint.setStrokeWidth(1);
		paint.setTextSize(12);
		cacheCanvas.drawText(""+formatDouble(max), 
				(float) 0,
				(float) (5+TABLE_HEIGHT*1.0/4), paint);
		cacheCanvas.drawText(""+formatDouble(min), 
				(float) 0, 
				(float) (TABLE_HEIGHT+15), paint);
		cacheCanvas.drawText(""+points_temp.size(),
				(float) (TABLE_WIDTH*1.0-30), 
				(float) (TABLE_HEIGHT+15), paint); 

	}
	
	private double formatDouble(double d){
		BigDecimal b = new BigDecimal(d);//BigDecimal 
		double db = b.setScale(BigDecimal.ROUND_CEILING,6).doubleValue();
		return db;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		/**
		 * 位置关系的处理
		 */	
		if(event.getX()>0&&event.getX()<TABLE_WIDTH&&event.getY()>0&&event.getY()<TABLE_HEIGHT){
			/** 处理单点、多点触摸 **/
			switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				onTouchDown(event);			
				break;
			// 多点触摸
			case MotionEvent.ACTION_POINTER_DOWN:
				onPointerDown(event);
				break;
			case MotionEvent.ACTION_MOVE:
				onTouchMove(event);
				break;
			case MotionEvent.ACTION_UP:
				mode = NONE;
				break;

			// 多点松开
			case MotionEvent.ACTION_POINTER_UP:
				mode = NONE;
				break;
			}
		}
		return true;

	}
	
	/**
	 * 单触点
	 * @param event
	 */
	private void onTouchDown(MotionEvent event) {
		// TODO Auto-generated method stub
		if(event.getPointerCount()==1){
			mode = MOVE;
			downX = event.getX();
			downY = event.getY();
		}	
	}
	
	/**
	 * 多触点
	 * @param event
	 */
	private void onPointerDown(MotionEvent event) {
		// TODO Auto-generated method stub
		if (event.getPointerCount() == 2) {
			mode = ZOOM;
			beforeLength = getDistance(event);// 获取两点的距离
		}
	}
	
	/**
	 * 移动
	 * @param event
	 */
	private void onTouchMove(MotionEvent event) {
		// TODO Auto-generated method stub
		if (mode == ZOOM) {
			afterLength = getDistance(event);// 获取两点的距离	
			double gapLenght = afterLength - beforeLength;// 变化的长度
			if (Math.abs(gapLenght) > 5f&&points_temp.size()!=0&&beforeLength!=0) {
				double scale_temp = afterLength/beforeLength;// 求的缩放的比例
				double middleX = getMiddleX(event);	//中点x坐标
				double middleY = getMiddleY(event);	//中点y坐标
				resetPoints(scale_temp,middleX,middleY);	//重设置
				this.invalidate();	//重新绘制
				beforeLength = afterLength;
			}
		}
		else if(mode == MOVE){
			//计算实际距离
			double moveX = event.getX() - downX;//X轴移动距离
			double moveY = event.getY() - downY;//y轴移动距离
			
			//计算相对距离
			double MoveX =  moveX*points_temp.size()/TABLE_WIDTH;//X轴移动的相对距离	
			double MoveY = moveY*(maxHDistance*4/3)/TABLE_HEIGHT;//y轴移动相对距离
			
			//设置移动的速度
			double speedX = MoveX*scale/5;
			double speedY = MoveY;
			
			if(!isBeyondBorder(speedX, speedY)){				
				//重设置
				for(int i=0;i<points_temp.size();i++){	
					Point p = points_temp.get(i);
					points_temp.get(i).setX(p.getX()+speedX);
					points_temp.get(i).setY(p.getY()-speedY);	
				}
			}
			//重绘
			this.invalidate();
			downX = event.getX();
			downY = event.getY();
		}
	}

	/**
	 * 
	 * @param event
	 * @return 获取两手指之间的距离
	 */
	private double getDistance(MotionEvent event){
		double x = event.getX(0) - event.getX(1);
		double y = event.getY(0) - event.getY(1);
		return Math.sqrt(x * x + y * y);
	}
	
	/**
	 * 
	 * @param event
	 * @return	得到视图的x坐标中点
	 */
	private double getMiddleX(MotionEvent event){
		return (event.getX(1)+event.getX(0))/2;
	}
	/**
	 * 
	 * @param event
	 * @return 得到视图的y坐标中点
	 */
	private double getMiddleY(MotionEvent event){
		return (event.getY(1)+event.getY(0))/2;
	}

	/**
	 * 重新设置点
	 * @param scale_temp
	 * @param middleX
	 * @param middleY
	 */
	private void resetPoints(double scale_temp,double middleX,double middleY){

		/**
		 * 缩放比例在最小比例和最大比例范围内
		 */
		if(scale*scale_temp>=scale_max){
			scale_temp = scale_max/scale;
			scale = scale_max;			
		}else if(scale*scale_temp<=scale_min){
			scale_temp = scale_min/scale;
			scale = scale_min;
		}else{
			scale = scale*scale_temp;
		}

		double MidX =  middleX*points_temp.size()/TABLE_WIDTH;
		//y值不变
		double MidY = (TABLE_HEIGHT-middleY)*maxHDistance*4/(3*TABLE_HEIGHT);

		/**
		 * 重新设置points_temp的值
		 */
		for(int i=0;i<points_temp.size();i++){		
			double tempX = points_temp.get(i).getX();
			points_temp.get(i).setX(MidX-(MidX-tempX)*scale_temp);
			//y值不变
			double tempY = points_temp.get(i).getY();	
			points_temp.get(i).setY(MidY-(MidY-tempY)*scale_temp);	
		}

	}

	
	/**
	 * @param speedX
	 * @param speedY
	 * @return 判断是否超出边界,这个方法写的可能存在问题
	 */
	private boolean isBeyondBorder(double speedX,double speedY ){
		boolean beyond = false;	//默认都超出界限

		if(mode == MOVE){
			//最左边的点
			Point leftP = points_temp.get(0);
			//最右边的点
			Point rightP = points_temp.get(points_temp.size()-1);
			
			if(leftP.getX()+speedX>=points_temp.size()){
				beyond = true;
//				Toast.makeText(mContext, "已移至最右端", Toast.LENGTH_SHORT).show();
			}else if(rightP.getX()+speedX<=0){
				beyond = true;
//				Toast.makeText(mContext, "已移至最左端", Toast.LENGTH_SHORT).show();
			}
			
			//最高点
			Point maxHPoint = points_temp.get(tempIndex);
			//最低点
			Point minHPoint = points_temp.get(0);
			
			if(maxHPoint.getY()-speedY<=0){
				beyond = true;
//				Toast.makeText(mContext, "已移至最低端", Toast.LENGTH_SHORT).show();
			}else if(minHPoint.getY()-speedY>=maxHDistance*4/3){
				beyond = true;
//				Toast.makeText(mContext, "已移至最高端", Toast.LENGTH_SHORT).show();
			}

		}
		return beyond;
	}











	/**
	 * 
	 * @param filebytes
	 */
	public void addPoints(byte [] filebytes){

		if(points_temp.size()!=0) points_temp.clear(); 
		scale = 1;


		for (int i = 6; i < filebytes.length-2; i = i + 3){
			Point p = new Point();
			p.setX((i - 6) / 3 + 1);
//			double y = DpByteToInt(new byte[] { filebytes[i], filebytes[i + 1], filebytes[i + 2] });
			double y = byteToDouble(new byte[] { filebytes[i], filebytes[i + 1], filebytes[i + 2] });
//			double dy = 0;
			if (y!= 0)
				y = (int) Math.log(y);
			p.setY(y);
			points_temp.add(p);		//将实例化后的point添加进来
		}

		maxHDistance = getMHeight();
	}
	
	
	/**
	 * 
	 * @param bs
	 * @return
	 */
	private double byteToDouble(byte[] bs){

		Byte b0 = new Byte(bs[0]);
		Byte b1 = new Byte(bs[1]);
		Byte b2 = new Byte(bs[2]);

		double d0 = b0.doubleValue();
		double d1 = b1.doubleValue();
		double d2 = b2.doubleValue();

		return d0*Math.pow(256,0)+d1*Math.pow(256,1)+d2*Math.pow(256,2); 

	}
	
	/**
	 * 
	 * @return 获取坐标的最大值
	 */
	private double getMHeight(){
		if(points_temp.size()<=0) return 0;
		double maxVal = points_temp.get(0).getY();
		for(int i=0;i<points_temp.size();i++){
			if(points_temp.get(i).getY()>maxVal){
				maxVal = points_temp.get(i).getY();
				tempIndex = i;
			}
				
		}
		if(maxVal==Double.POSITIVE_INFINITY) return 0;
		return maxVal;
	}
	
	/**
	 * 
	 * @param filepath
	 * @return	得到字节数组
	 */
	public byte[] readFromPath(String filepath){
		InputStream inputStream = null;
		DataInputStream dis = null;
		byte [] arr_6152 = null;
		byte data = 0;
		int i = 0;
		try {
			inputStream = mContext.getAssets().open(filepath);
			dis = new DataInputStream(inputStream);

			arr_6152 = new byte[6152];

			while((data=dis.readByte())!=-1){	
				arr_6152[i] = data;	
				i++;
			}

			inputStream.close();
			return arr_6152;
		} 
		catch(EOFException e){
			return arr_6152;
		}catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

	
	public double getvWidth() {
		return vWidth;
	}


	public void setvWidth(double vWidth) {
		this.vWidth = vWidth;
	}


	public double getvHeight() {
		return vHeight;
	}


	public void setvHeight(double vHeight) {
		this.vHeight = vHeight;
	}
}
