import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;


public class iAuctionListener extends PluginListener{
	Logger log = Logger.getLogger("Minecraft");
	int maxtime;
	int mintime;
	int time;
	double maxprice;
	double minprice;
	double price;
	boolean allowcreative;
	boolean adminallow;
	String disalloweditems;
	String currency = (String)etc.getLoader().callCustomHook("dCBalance", new Object[] { "MoneyName" });
	HashMap<String,Item> auction = new HashMap<String,Item>();
	HashMap<String,Double> bidders = new HashMap<String,Double>();
	List<Integer> disalloweditemslist;
	
	
	public void loadprops(){
		PropertiesFile props = new PropertiesFile("plugins/config/iAuction.properties");
		maxtime = props.getInt("Max-Auction-Time" , 100);
		mintime = props.getInt("Minimal-Auction-Time" , 10);
		minprice = props.getDouble("Minimal-Price",10);
	    maxprice = props.getDouble("Maximal-Price",10000);
		allowcreative = props.getBoolean("Allow-Creative-Auction",false);
		adminallow = props.getBoolean("Allow-Admins-To-Bid",false);
		disalloweditems = props.getString("Disallowed-Items","7,8,9,10,11");
	}
	
	
	public boolean onCommand(Player player, String[] split) {
		if (split[0].equalsIgnoreCase("/auction")&&player.canUseCommand("/auction")){//auction time price
			if (split.length <3 || split.length >3){
				player.sendMessage("�f[�aiAuction�f]�c The correct usage is '/auction <time> <price>'");
				return true;
			}
			
			if (!auction.isEmpty()){
				player.sendMessage("�f[�aiAuction�f]�c Please wait till the current auction is ended!");
				return true;
			}
			
			try{
				time = Integer.parseInt(split[1]);  
				price = Double.parseDouble(split[2]);
				}catch(NumberFormatException nfe){
					player.sendMessage("�f[�aiAuction�f]�c The correct usage is '/auction <time> <price>'"); 
					return true;
					}
			
			if (player.getItemStackInHand() == null){
				player.sendMessage("�f[�aiAuction�f]�c Please hold the item you want to auction!");
				return true;
			}
		       Item iih = player.getItemStackInHand();
		       if (disalloweditems.contains(iih.getItemId()+"")){
				player.sendMessage("�f[�aiAuction�f]�c You cant auction this item!");
				return true;
			}
			
			if (player.getCreativeMode() == 1){
				if (!allowcreative){
					player.sendMessage("�f[�aiAuction�f]�c You are not allowed to auction items in creative mode!");
					return true;
				}
			}
			
			if (maxtime != 0){
			if (time > maxtime){
				player.sendMessage("�f[�aiAuction�f]�c You exceeded the time limit of "+maxtime+" seconds!");
				return true;
			}
			}

			if (mintime != 0){
				if (time < mintime){
					player.sendMessage("�f[�aiAuction�f]�c The minimal time for an auction is "+mintime+" seconds!");
					return true;
				}
			}

			if (minprice != 0){
				if (price > maxprice){
					player.sendMessage("�f[�aiAuction�f]�c You exceeded the price limit of "+maxprice+" "+currency+"!");
					return true;
				}
			}

			if (maxprice != 0){
				if (price < minprice){
					player.sendMessage("�f[�aiAuction�f]�c The minimal price for an auction is "+minprice+" "+currency+"!");
					return true;
				}
			}
			auction.put(player.getName(), iih);
			bidders.put(player.getName(), price);
			runauction(time,player,iih);
			player.getInventory().removeItem(player.getItemStackInHand().getSlot());
			return true;
		}
		if (split[0].equalsIgnoreCase("/bid")&&player.canUseCommand("/auctionbid")){
			double bid;
			if (split.length <2 || split.length >2){
				player.sendMessage("�f[�aiAuction�f]�c The correct usage is '/bid <price>'");
				return true;
			}
			if (auction.isEmpty()){
				player.sendMessage("�f[�aiAuction�f]�c There is no auction running!");
				return true;
			}
			List<Double> list = new ArrayList<Double>(bidders.values());
			try{bid = Double.parseDouble(split[1]);}catch(NumberFormatException nfe){player.sendMessage("�f[�aiAuction�f]�c The correct usage is '/bid <price>'"); return true;}
			if (bid < list.get(0)){
				player.sendMessage("�f[�aiAuction�f]�c Your bid must be higher than "+list.get(0)+" "+currency);
				return true;
			}
			double balance =(Double)etc.getLoader().callCustomHook("dCBalance", new Object[] { "Player-Balance", player.getName()});
			
			if (balance < bid){
				player.sendMessage("�f[�aiAuction�f]�c You dont have enough money to pay this bid");
				return true;
			}
			
			List<String> ownerc = new ArrayList<String>(auction.keySet());
			if (ownerc.get(0).equals(player.getName())){
				player.sendMessage("�f[�aiAuction�f]�c You cant bid on your own auction!");
				return true;
			}
			
			List<String> list1 = new ArrayList<String>(bidders.keySet());
			if (list1.get(0).equals(player.getName())){
			   player.sendMessage("�f[�aiAuction�f]�c you've just offered in this auction");
			   return true;
			}
			if (player.isAdmin() && !adminallow){
				player.sendMessage("�f[�aiAuction�f]�c Admins are not allowed to bid!");
				return true;
			}
			setbid(player,bid);
			etc.getServer().messageAll("�f[�aiAuction�f]�3 5 �3"+player.getName()+" �1 Has just offered  �6"+bid+" �1"+currency);
			return true;
		}
		if (split[0].equalsIgnoreCase("/auctioninfo")&&player.canUseCommand("/auction")){
			if (auction.isEmpty()){
				player.sendMessage("�f[�aiAuction�f]�c There is no auction running!");
				return true;
			}
			List<String> list1 = new ArrayList<String>(auction.keySet());
			ArrayList<String> dat = itemdata(auction.get(list1.get(0)));
			List<String> bidderss = new ArrayList<String>(bidders.keySet());
			player.sendMessage("�f[�aiAuction�f]�1 Auction Host: �3"+list1.get(0));
			player.sendMessage("�f[�aiAuction�f]�1 Auction Item Name: �3"+dat.get(0));
			player.sendMessage("�f[�aiAuction�f]�1 Auction Item Id: �3"+dat.get(1));
			player.sendMessage("�f[�aiAuction�f]�1 Auction Item Amount: �3"+dat.get(2));
			player.sendMessage("�f[�aiAuction�f]�1 Auction Item Damage: �3"+dat.get(3));
			player.sendMessage("�f[�aiAuction�f]�1 Auction Item Enchantments: �3"+dat.get(4));
			player.sendMessage("�f[�aiAuction�f]�1 Auction Item Price: �3"+bidders.get(bidderss.get(0)));
			player.sendMessage("�1-----------------Bids info-----------------");
			if (bidderss.get(0).equals(list1.get(0))){
			player.sendMessage("�f[�aiAuction�f]�1 Auction Bidder: �3No bids");	
			player.sendMessage("�f[�aiAuction�f]�1 Auction Price: �3No bids");	
			return true;
			}else{
				player.sendMessage("�f[�aiAuction�f]�1 Auction Bidder: �3"+bidderss.get(0));	
				player.sendMessage("�f[�aiAuction�f]�1 Auction Price: �3"+bidders.get(bidderss.get(0)));
				return true;
			}
		}
		return false;
	}
	
	public void runauction(final int time, final Player player, final Item item){
		ArrayList<String> id = itemdata(item);
		if (item.getEnchantment() == null){
		etc.getServer().messageAll("�f[�1iAuction�f] - �3"+player.getName()+"�1 Is selling �3"+id.get(2)+ " " +id.get(0)+"�1 for �6"+ price +" �1"+currency+" (damage "+ id.get(3)+")");
		}else{
	    etc.getServer().messageAll("�f[�1iAuction�f] - �3"+player.getName() +"�1 Is selling �3"+id.get(2)+" "+ id.get(0)+"�1 With �3"+ id.get(4) +"�1 for �6"+ price +" �1"+currency+" (damage "+ id.get(3)+")");
		}
	    new Thread() {
	   	     public void run() {
	   	          try{
		             int run = (time-5)*1000;
	   	        	  Thread.sleep(run);
	   	        	etc.getServer().messageAll("�f[�aiAuction�f]�3 5 �1Seconds left to bid!");
	   	        	  Thread.sleep(1000);
	   	        	etc.getServer().messageAll("�f[�aiAuction�f]�3 4 �1Seconds left to bid!");
	   	        	  Thread.sleep(1000);
	   	        	etc.getServer().messageAll("�f[�aiAuction�f]�3 3 �1Seconds left to bid!");
	   	        	  Thread.sleep(1000);
	   	        	etc.getServer().messageAll("�f[�aiAuction�f]�3 2 �1Seconds left to bid!");
	   	        	  Thread.sleep(1000);
	   	        	etc.getServer().messageAll("�f[�aiAuction�f]�3 1 �1Seconds left to bid!");
	   	        	  Thread.sleep(1000);
	   	        	etc.getServer().messageAll("�f[�aiAuction�f] �1- �1Auction ended.");
	   	        	
	   	        	if (bidders.containsKey(player.getName())){
	   	        		etc.getServer().messageAll("�f[�aiAuction�f] �1--Auction ended with no bids--");
	   	        		returnitem(player,item);	
	   	        		dropdata();
	   	        	}else{
	   	        		if (checkscam()){return;}
	   	        		List<String> list = new ArrayList<String>(bidders.keySet());
	   	        		List<Double> prc = new ArrayList<Double>(bidders.values());
	   	        		etc.getServer().messageAll("�f[�aiAuction�f] �1AuctionWinner is �3"+list.get(0)+" �6"+prc.get(0)+" �1"+currency);
	   	        		sellitem(player,item);
	   	        		dropdata();
	   	        	}
	   	        }catch(InterruptedException e) {player.sendMessage("�f[�aiAuction�f] �c an error occured report it to an admin!");}
	   	     }
	    }.start();
	}
	
	public void returnitem(Player player ,Item item){
		int slot = player.getInventory().getEmptySlot();
		if (slot == -1 || slot >= 100){
			player.getWorld().dropItem(player.getLocation(), item);
			player.sendMessage("�f[�aiAuction�f] �1 No space in inventory found (dropping item)!");
		}else{
			player.getInventory().setSlot(item, slot);
			player.sendMessage("�f[�aiAuction�f] �1 item given!");
		}
	}
	
	public void sellitem(Player seller,Item item){
		List<String> list = new ArrayList<String>(bidders.keySet());
		Player buyer = etc.getServer().matchPlayer(list.get(0));
		double prc = bidders.get(buyer.getName());
	      etc.getLoader().callCustomHook("dCBalance", new Object[] { "Player-Charge", buyer.getName(), (Double) prc});
	      etc.getLoader().callCustomHook("dCBalance", new Object[] { "Player-Pay", seller.getName(), (Double) prc});
	      returnitem(buyer,item);
	}
	
	public void dropdata(){
		if (!auction.isEmpty()){
		auction.clear();
		if (!bidders.isEmpty()){
			bidders.clear();
		}
		}else{
		if (!bidders.isEmpty()){
			bidders.clear();
		}
		}
	}
	
	public void setbid(Player player,double price) {
		if (!bidders.isEmpty()){
			bidders.clear();
			bidders.put(player.getName(), price);
		}else{
		bidders.put(player.getName(), price);
		}
	}
	
	public void tolist() throws Exception {
		if (disalloweditems == null || disalloweditems.equals("")){return;}
		if (!disalloweditems.contains(",")){disalloweditemslist.add(Integer.parseInt(disalloweditems)); return;}
		String[] sarray = disalloweditems.split(",");
		if (sarray != null) {
		for (int i = 0; i < sarray.length; i++) {
		disalloweditemslist.add(Integer.parseInt(sarray[i]));
		log.info(disalloweditemslist+"");
		}
		return;
		}
		return;
		}
	
	public static ArrayList<String> itemdata(Item item){
		ArrayList<String> r = new ArrayList<String>();
		String name,id, amount, damage, enchantments;
		name = getname(item);
		id = item.getItemId()+"";
		amount = item.getAmount()+"";
		damage = item.getDamage()+"";
			StringBuilder sb = new StringBuilder();
			if (item.getEnchantment() != null){
			for(Enchantment enchantment : item.getEnchantments()){
				sb.append(enchantment.getType().toString()+ " ");
				sb.append(enchantment.getLevel() +" ");
			}
			enchantments = sb.toString();
		}else{
		enchantments = "";
		}
    r.add(0, name);	
	r.add(1, id);	
	r.add(2, amount);	
	r.add(3, damage);	
	r.add(4, enchantments);	
	return r;
	}
	
	public boolean checkscam(){
			List<String> bidder = new ArrayList<String>(bidders.keySet());
			List<String> seller = new ArrayList<String>(auction.keySet());
			Item item = auction.get(seller.get(0));
			String Bplayer = bidder.get(0);
			String Splayer = seller.get(0);
			
			double balance =(Double)etc.getLoader().callCustomHook("dCBalance", new Object[] { "Player-Balance", Bplayer});
			double prices = bidders.get(Bplayer);
			
			Player BPlayer = etc.getServer().matchPlayer(Bplayer);
			Player SPlayer = etc.getServer().matchPlayer(Splayer);
	        if (prices > balance){
				etc.getServer().messageAll("�f[�aiAuction�f] �3"+Bplayer +"�c TRIED TO SCAM! RESTARTING AUCTION!");
				BPlayer.kick("�cDont try to scam ever again...!");
				etc.getServer().messageAll("�f[�aiAuction�f] �3"+Bplayer +"�c IS KICKED FROM THE SERVER!");
				dropdata();
				bidders.put(Splayer, price);
	   		    runauction(time,SPlayer,item);	
	   		    return true;
		}
		return false;
	}
	
	public static String getname(Item item){
		switch(item.getItemId()){
			case 5:
				switch(item.getDamage()){
					case 0: return "Oak Planks";
					case 1: return "Spruce Planks";
					case 2: return "Birch Planks";
					case 3: return "Jungle Planks";
				}
			case 6:
				switch(item.getDamage()){
					case 0: return "Oak Sapling";
					case 1: return "Spruce Sapling";
					case 2: return "Birch Sapling";
					case 3: return "Jungle Tree Sapling";
				}
			case 17:
				switch(item.getDamage()){
					case 0: return "Oak Log";
					case 1: return "Spruce Log";
					case 2: return "Birch Log";
					case 3: return "Jungle Log";
				}
			case 18:
				switch(item.getDamage()){
					case 0: return "OakLeaves";
					case 1: return "Spruce Leaves";
					case 2: return "Birch Leaves";
					case 3: return "Jungle Leaves";
				}
			case 24:
				switch(item.getDamage()){
					case 0: return "Normal Sandstone";
					case 1: return "Chiseled Sandstone";
					case 2: return "Smooth Sandstone";
				}
			case 31:
				switch(item.getDamage()){
					case 0: return "Dead Shrub";
					case 1: return "Tall Grass";
					case 2: return "Fern";
				}
			case 35:
				return item.getColor().getName()+" Wool";
			case 43:
				switch(item.getDamage()){
					case 0: return "DoubleStone Slab";
					case 1: return "DoubleSandstone Slab";
					case 2: return "DoubleWooden Stone Slab";
					case 3: return "DoubleCobblestone Slab";
					case 4: return "DoubleBrick Slab";
					case 5: return "DoubleStone Brick Slab";
					case 5: return "DoubleStone Slab";
				}		
			case 44:
				switch(item.getDamage()){
					case 0: return "Stone Slab";
					case 1: return "Sandstone Slab";
					case 2: return "Wooden Stone Slab";
					case 3: return "Cobblestone Slab";
					case 4: return "Brick Slab";
					case 5: return "Stone Brick Slab";
				}
			case 97:
				switch(item.getDamage()){
					case 0: return "SilverFish Stone";
					case 1: return "SilverFish CobbleStone";
					case 2: return "SilverFish Stonebrick";
				}
			case 98:
				switch(item.getDamage()){
					case 0: return "Normal Stonebrick";
					case 1: return "Mossy Stonebrick";
					case 2: return "Cracked Stonebrick";
					case 3: return "Chiseled Stonebrick";
				}
			case 263:
				switch(item.getDamage()){
					case 0: return "Coal";
					case 1: return "Charcoal";			
				}
			case 322:
				switch(item.getDamage()){
					case 0: return "Normal Golden Apple";
					case 1: return "Enchanted Golden Apple";
				}
			case 351:
				switch(item.getDamage()){
					case 0:return "Ink Sac";
            		case 1:return "Rose Red";
            		case 2:return "Cactus Green";
            		case 3:return "Cocoa Beans";
					case 4:return "Lapis Lazuli";
            		case 5:return "Purple Dye";
            		case 6:return "Cyan Dye";
					case 7:return "Light Gray Dye";
					case 8:return "Gray Dye";
            		case 9:return "Pink Dye";
            		case 10:return "Lime Dye";
            		case 11:return "Dandelion Yellow";
					case 12:return "Light Blue Dye";
            		case 13:return "Magenta Dye";
            		case 14:return "Orange Dye";
					case 15:return "Bone Meal";
				}
			case 373:
				switch(item.getDamage()){ //Just want to point out that these values aren't exactly right, Brewed potions have values in the ranges of 8100 and 16300
					case 0:return "Water Bottle";
					case 1:return "Potion of Regeneration";
        			case 2:return "Potion of Swiftness";
					case 3:return "Potion of Fire Resistance";
					case 4:return "Potion of Poison";
					case 5:return "Potion of Healing";
					case 6:return "Clear Potion";
					case 7:return "Clear Potion";
					case 8:return "Potion of Weakness";
					case 9:return "Potion of Strength";
					case 10:return "Potion of Slowness";
					case 11:return "Diffuse Potion";
					case 12:return "Potion of Harming";
					case 13:return "Artless Potion";
        			case 14:return "Thin Potion";
        			case 15:return "Thin Potion";
					case 16:return "Awkward Potion";
        			case 17:return "Potion of Regeneration";
        			case 18:return "Potion of Swiftness";
        			case 19:return "Potion of Fire Resistance";
        			case 20:return "Potion of Poison";
        			case 21:return "Potion of Healing";
        			case 22:return "Bungling Potion";
        			case 23:return "Bungling Potion";
        			case 24:return "Potion of Weakness";
        			case 25:return "Potion of Strength";
        			case 26:return "Potion of Slowness";
        			case 27:return "Smooth Potion";
        			case 28:return "Potion of Harming";
        			case 29:return "Suave Potion";
        			case 30:return "Debonair Potion";
        			case 31:return "Debonair Potion";
        			case 32:return "Thick Potion";
        			case 33:return "Potion of Regeneration II";
        			case 34:return "Potion of Swiftness II";
        			case 35:return "Potion of Fire Resistance";
        			case 36:return "Potion of Poison II";
        			case 37:return "Potion of Healing II";
        			case 38:return "Charming Potion";
        			case 39:return "Charming Potion";
        			case 40:return "Potion of Weakness";
        			case 41:return "Potion of Strength II";
        			case 42:return "Potion of Slowness";
        			case 43:return "Refined Potion";
        			case 44:return "Potion of Harming II";
        			case 45:return "Cordial Potion";
        			case 46:return "Sparkling Potion";
        			case 47:return "Sparkling Potion";
        			case 48:return "Potent Potion";
        			case 49:return "Potion of Regeneration II";
        			case 50:return "Potion of Swiftness II";
        			case 51:return "Potion of Fire Resistance";
        			case 52:return "Potion of Poison II";
        			case 53:return "Potion of Healing II";
        			case 54:return "Rank Potion";
        			case 55:return "Rank Potion";
        			case 56:return "Potion of Weakness";
        			case 57:return "Potion of Strength II";
        			case 58:return "Potion of Slowness";
        			case 59:return "Acrid Potion";
        			case 60:return "Potion of Harming II";
        			case 61:return "Gross Potion";
        			case 62:return "Stinky Potion";
        			case 63:return "Stinky Potion";
        		}
			case 383:
				switch(item.getDamage()){
					case 50:return "Creeper Egg";
					case 51:return "Skeleton Egg";
					case 52:return "Spider Egg";
					case 54:return "Zombie Egg";
					case 55:return "Slime Egg";
					case 56:return "Ghast Egg";
					case 57:return "Zombie Pigman Egg";
					case 58:return "Enderman Egg";
					case 59:return "Cave Spider Egg";
					case 60:return "Silverfish Egg";
					case 61:return "Blaze Egg";
					case 62:return "Magma Cube Egg";
					case 90:return "Pig Egg";
					case 91:return "Sheep Egg";
					case 92:return "Cow Egg";
					case 93:return "Chicken Egg";
					case 94:return "Squid Egg";
					case 95:return "Wolf Egg";
					case 96:return "Mooshroom Egg";
					case 98:return "Ocelot Egg";
					case 120:return "Villager Egg";
				}
			default: return item.getType().toString();
		}
	}
}