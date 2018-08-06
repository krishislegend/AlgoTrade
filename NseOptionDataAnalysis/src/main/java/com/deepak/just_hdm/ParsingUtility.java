package com.deepak.just_hdm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

public class ParsingUtility {
	public SessionFactory factory;

	public ParsingUtility(SessionFactory factory) {
		this.factory = factory;
	}
	
	
	public static String parseValueFromColumn(Elements strikeRow, int i) {
		String value;
		value = strikeRow.get(i).getAllElements().get(0).childNode(0).toString().replaceAll(",","");
		if(value.equalsIgnoreCase(" ") || value.equalsIgnoreCase("-"))
			value = "0";
		return value;
	}
	
	public static void persistOptionBean(Session s, StringBuffer sb, String seriesName,
			Map<String, Map<String, OptionBean>> obMap,Set<Double> strikePriceLSet,Map<String,LiveRate> liveRateMap,int lotSize) {
		Transaction t;
		org.jsoup.nodes.Document document = Jsoup.parse(sb.toString());
		org.jsoup.nodes.Element tableCMP = document.select("table").get(0);
		org.jsoup.nodes.Element cmpElement = tableCMP.select("b").get(0);
		org.jsoup.nodes.Node cmElement = cmpElement.childNode(0);
		StringTokenizer st = new StringTokenizer(cmElement.toString(), " ");
		String scripName =st.nextToken();
		LiveRate liveRate = new LiveRate();
		liveRate.setScripName(scripName);
		liveRate.setCurrentMktPrice(new Double(st.nextToken()));
		liveRate.setLotsize(lotSize);
		liveRateMap.put(liveRate.getScripName(), liveRate);
		
		
		
		
		
		org.jsoup.nodes.Element table = document.select("table").get(2);
		
		List<OptionBean> obList = new ArrayList<OptionBean>();
		
		org.jsoup.nodes.Element e2 = table.child(1);
		
		Elements optionRows = e2.select("tr");
		
		int rowSize = optionRows.size();
		int rowCount =0;
		for(org.jsoup.nodes.Element tr :  optionRows){
			
			if(++rowCount>=rowSize)
				break;
			
			Elements strikeRow = tr.select("td");
			
				int columnIndex= 1;
				OptionBean ob = new OptionBean();
				String optionType = "CE";
				Map<String, OptionBean> innerMap = new HashMap<String, OptionBean>();
				
				
				ob.setCmp(liveRate.getCurrentMktPrice());
				ob.setOi(new Double(parseValueFromColumn(strikeRow, columnIndex++)));
				ob.setChangeInOi(new Double(parseValueFromColumn(strikeRow, columnIndex++)));
				ob.setVolume(new Double(parseValueFromColumn(strikeRow, columnIndex++)));
				ob.setIv(new Double(parseValueFromColumn(strikeRow, columnIndex++)));
				
				
				org.jsoup.nodes.Element strikeRow1 = null;
				String value2 =null;
				
				if(strikeRow.get(columnIndex).getAllElements().get(0).select("a")!=null &&  
						strikeRow.get(columnIndex).getAllElements().get(0).select("a").size()>0){
					
					strikeRow1 =  strikeRow.get(columnIndex).getAllElements().get(0).select("a").get(0);
					
					 value2 = strikeRow1.childNode(0).toString().replaceAll(",", "");
					
					ob.setLtp(new Double(value2));
				}
				columnIndex++;
				ob.setNetChng(new Double(parseValueFromColumn(strikeRow, columnIndex++)));
				ob.setBidQty(new Double(parseValueFromColumn(strikeRow, columnIndex++)));
				ob.setBidPrice(new Double(parseValueFromColumn(strikeRow, columnIndex++)));
				ob.setAskPrice(new Double(parseValueFromColumn(strikeRow, columnIndex++)));
				ob.setAskQty(new Double(parseValueFromColumn(strikeRow, columnIndex++)));
				
				org.jsoup.nodes.Element strikeRow2 =  strikeRow.get(columnIndex++).getAllElements().get(0).select("b").first();
				
				ob.setStrikePrice(new Double(strikeRow2.childNode(0).toString().replaceAll(",", "")));
				ob.setOptionType(optionType);
				ob.setSeriesName(seriesName);
				ob.setScripName(scripName);
				obList.add(ob);
				
				innerMap.put(optionType, ob);
				
				
				double tempStrikePrice = ob.getStrikePrice();
				
			// Put Option
				ob = new OptionBean();
				optionType= "PE";
				ob.setCmp(liveRate.getCurrentMktPrice());
				ob.setBidQty(new Double(parseValueFromColumn(strikeRow, columnIndex++)));
				ob.setBidPrice(new Double(parseValueFromColumn(strikeRow, columnIndex++)));
				ob.setAskPrice(new Double(parseValueFromColumn(strikeRow, columnIndex++)));
				ob.setAskQty(new Double(parseValueFromColumn(strikeRow, columnIndex++)));
				ob.setNetChng(new Double(parseValueFromColumn(strikeRow, columnIndex++)));
				
				
				if(strikeRow.get(columnIndex).getAllElements().get(0).select("a")!=null &&  
						strikeRow.get(columnIndex).getAllElements().get(0).select("a").size()>0){
					
					strikeRow1 =  strikeRow.get(columnIndex).getAllElements().get(0).select("a").get(0);
					
					 value2 = strikeRow1.childNode(0).toString().replaceAll(",", "");
					
					ob.setLtp(new Double(value2));
				}
				columnIndex++;
				
				
				ob.setIv(new Double(parseValueFromColumn(strikeRow, columnIndex++)));
				
				ob.setVolume(new Double(parseValueFromColumn(strikeRow, columnIndex++)));
				ob.setChangeInOi(new Double(parseValueFromColumn(strikeRow, columnIndex++)));
				ob.setOi(new Double(parseValueFromColumn(strikeRow, columnIndex++)));
				ob.setOptionType(optionType);
				ob.setStrikePrice(tempStrikePrice);
				ob.setSeriesName(seriesName);
				ob.setScripName(scripName);
				
				innerMap.put(optionType, ob);
				
				obMap.put(String.valueOf(seriesName+"_"+ ob.getStrikePrice()), innerMap);
				strikePriceLSet.add(new Double(ob.getStrikePrice()));
				
				obList.add(ob);
		}
			
		
		
     //  t = s.beginTransaction();
        for(OptionBean o : obList){
       	 
       	 s.saveOrUpdate(o);
        }
     //  t.commit();
	}
	
	
	public static void persistOptionCalendarTrade(Session s, Map<String, Map<String, OptionBean>> obMap,
			String scripName, final String currentSeries, final String nextSeries, final int lotSize,
			final String callOptionType, final String putOptionType, Set<Double> strikePriceSet) {
		Transaction t;
		List<OptionCalendarTrade> ocList = new ArrayList<OptionCalendarTrade>();
		
		OptionBean optionCurrentSeriesCall=null;
		OptionBean optionCurrentSeriesPut=null;
		OptionBean optionNextSeriesCall = null;
		OptionBean optionNextSeriesPut =null;
		for(Double strikePrice: strikePriceSet){
			
			Map<String, OptionBean> map = obMap.get(currentSeries+"_"+ strikePrice.doubleValue());
			
			if(map!=null){
				optionCurrentSeriesCall = map.get(callOptionType);
				optionCurrentSeriesPut = map.get(putOptionType);
			}else{
				continue;
			}
			
			
			map = obMap.get(nextSeries+"_"+ strikePrice.doubleValue());
			
			if(map!=null){
				optionNextSeriesCall = map.get(callOptionType);
				optionNextSeriesPut = map.get(putOptionType);
			}else{
				continue;
			}
			
			OptionCalendarTrade opt = populateOptionCalendarTradeBean(scripName, lotSize, callOptionType,
					strikePrice, optionCurrentSeriesCall, optionNextSeriesCall);
			ocList.add(opt);
			
			 opt = populateOptionCalendarTradeBean(scripName, lotSize, putOptionType,
					 strikePrice, optionCurrentSeriesPut, optionNextSeriesPut);
			ocList.add(opt);
			
			
		}

		 t = s.beginTransaction();
		    for(OptionCalendarTrade o : ocList){
		   	 
		   	 s.saveOrUpdate(o);
		    }
		   t.commit();
	}




	public static OptionCalendarTrade populateOptionCalendarTradeBean(String scripName, final int lotSize,
			String optionType, double strikePriceKey, OptionBean optionCurrentSeries, OptionBean optionNextSeries) {
		OptionCalendarTrade opt = new OptionCalendarTrade();
		
		
		opt.setCmp(optionCurrentSeries.getCmp());
		opt.setStrikePrice(strikePriceKey);
		opt.setScripName(scripName);
		opt.setOptionType(optionType);
		opt.setPoint(optionNextSeries.getAskPrice()- optionCurrentSeries.getBidPrice());
		opt.setLotSize(lotSize);
		opt.setPrice(opt.getPoint()*opt.getLotSize());
		opt.setCurrentSeriesBidPrice(optionCurrentSeries.getBidPrice());
		opt.setCurrentSeriesAskPrice(optionCurrentSeries.getAskPrice());
		opt.setNextSeriesAskPrice(optionNextSeries.getAskPrice());
		opt.setNextSeriesBidPrice(optionNextSeries.getBidPrice());
		opt.setNextSeriesIv(optionNextSeries.getIv());
		opt.setCurrenSeriestIv(optionCurrentSeries.getIv());
		opt.setCurrentSeriesOI(optionCurrentSeries.getOi());
		opt.setNextSeriesOI(optionNextSeries.getOi());
		double bidAskDifference = optionNextSeries.getAskPrice() -optionNextSeries.getBidPrice();
		
		if((optionNextSeries.getAskPrice()-bidAskDifference/2 ) != 0)
			opt.setRewardRiskTentative(optionCurrentSeries.getBidPrice()/(optionNextSeries.getAskPrice()-bidAskDifference/2 ));
		else 
			opt.setRewardRiskTentative(AppConstant.NAN);
		
		if(optionNextSeries.getAskPrice()!=0)
			opt.setRewardRisk(optionCurrentSeries.getBidPrice()/optionNextSeries.getAskPrice());
		else
			opt.setRewardRisk(AppConstant.NAN);
		return opt;
	}
	
	public static void getURLAction(StringBuffer sb, String charset,String series, String scripName, String instrumentType) {
		URLConnection connection;
		BufferedReader br = null;
		try {
			//connection = new URL("https://www.nseindia.com/live_market/"
				//	+ "dynaContent/live_watch/option_chain/optionKeys.jsp?symbolCode=-9999&symbol=NIFTY&symbol=BANKNIFTY&instrument=OPTIDX&date=-&segmentLink=17&segmentLink=17").openConnection();
			
			connection = new URL("https://www.nseindia.com/live_market/dynaContent/live_watch/option_chain/optionKeys.jsp?"
					+ "segmentLink=17&instrument="+instrumentType  + "&symbol="+scripName+"&date="+series).openConnection();
			
			connection.setDoOutput(true);
			connection.setRequestProperty("Accept-Charset", "UTF-8");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + "UTF-8");
			
			/*try {
				
			    connection.getOutputStream().write("".getBytes(charset));
			}
			finally {
			    connection.getOutputStream().close();
			}*/
			String line="";
			connection.setReadTimeout(6000);
			InputStream response1 = connection.getInputStream();
			br = new BufferedReader(new InputStreamReader(response1));
			while ((line = br.readLine()) != null) {
				sb.append(line+"\n");
			}
			//System.out.println(sb.toString());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			
			if(br!=null)
				try {
					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}
	
}