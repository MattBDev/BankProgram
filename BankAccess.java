import java.net.*;
import java.io.*;

public class BankAccess implements Runnable {
	
	boolean dir;
	
	BufferedReader br;
	BufferedWriter bw;
	
	

	private class Account {
		
		private MoneyFloat bal;
		private int pin;
		private String name;
		
		public Account(float bal, int pin, String name) throws BankException {

			this.bal = new MoneyFloat(bal);
			this.pin = pin;
			this.name = name;
		}
		
		public void deposit(float amt) throws BankException {
			System.out.println("amt: " + amt);
			MoneyFloat a = new MoneyFloat(amt);
			System.out.println("after: " + a.num + ", pos: " + a.isPositive());
			if (a.isPositive()) {
				bal = bal.sum(a);
			} else {
				//This is being called erroneously. Why?
				throw new BankException("Deposits must be greater than zero");
			}
		}
		
		//throw withdraw error: wrong pin, or invalid amount
		public void withdraw(float amt) throws BankException {
			MoneyFloat a = new MoneyFloat(amt);
			if (a.isPositive()) {
				if (bal.compare(a) >= 0) {
					bal = bal.sum(a.flip());
				} else {
					throw new BankException("Insufficient funds.");
				}
			} else {
				throw new BankException("Deposits must be greater than zero");
			}

		}
		
		public String getName() {
			return name;
		}
		
		//TODO: maybe remove this method
		public int getPin() {
			return pin;
		}
		
		public float getBal() {
			return bal.num;
		}
		
	}
	
	private class MoneyFloat {
		
		public Float num;
		
		public MoneyFloat(Float f) throws BankException {
			num = f;
			validate();
		}
		
		public void validate() throws BankException {
			num = new Float(String.format(java.util.Locale.US, "%.2f", num));
			
			if (num >= 1000000) {
				throw new BankException("No monetary amount may exist greater than one million.");
			}
		}
		
		public MoneyFloat flip() throws BankException {
			num *= -1;
			validate();
			return this;
		}
		
		public MoneyFloat sum(MoneyFloat f) throws BankException {
			MoneyFloat r = new MoneyFloat(num + f.num);
			System.out.println("Added: " + r.num);
			if (f.isPositive()) {
				if (num >= r.num) {
					throw new BankException("Error: funds too large to deposit this amount.");
				}
			} else {
				System.out.println("Before: " + num);
				System.out.println("After: " + r.num);
				if (num <= r.num) {
					throw new BankException("Error: withdraw request too large.");
				}
			}
			return r;
		}
		
		public int compare(MoneyFloat f) {
			if (f.num < num) {
				return 1;
			} else if (f.num > num) {
				return -1;
			}
			return 0;
		}
		
		public boolean isPositive() {
			return num > 0;
		}

	}
	
	private class BankException extends Exception {
		public BankException(String msg) {
			super(msg);
		}
	}
	

	public BankAccess(Socket s, boolean direct) throws IOException {
		dir = direct;
		
		br = new BufferedReader(new InputStreamReader(s.getInputStream()));
		bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
	}
	
	@Override
	public void run() {
				
		while (true) {
		
			write("hello");
			
			String in = read();
			
			String cmd[] = in.split("\\s+");
			Account acct = null;
			
			if (cmd.length > 1) {
				try {
					acct = buildAccount(cmd[1]);
				} catch (BankException e) {
					write(e.getMessage());
				}
			} else {
				write("Input command is not in a recognized form. Commands require at least two arguments");
			}
			
			
			if (acct != null) {
				try {
					parseCommand(cmd, acct);
				} catch (BankException e) {
					write(e.getMessage());
				}
			} else {
				write("Account not found");
			}
		
		}
				
	}
	
	private <T extends Number> T parseLine(String[] in, String match) throws BankException {
		String error = "Error: Account file formatted incorrectly";
		if (in.length == 2) {
			try {
				switch (in[0]) {
					case "pin:":
						T i = (T)Integer.valueOf(in[1]);
						return i;
					case "bal:":
						T f = (T)Float.valueOf(in[1]);
						return f;
				}
			} catch (NumberFormatException e) {
				throw new BankException(error + ". PIN or Balance is in the wrong format.");
			}
		}
		throw new BankException(error);
	}
	
	private Account buildAccount(String name) throws BankException {
				
		BufferedReader in = null;
		String s_bal[];
		String s_pin[];
		
		try {
			//TODO: be more careful about what files are opened
			//(Attempt to) prevent them from opening any other potentially malicious files
			if (!(name.equals("Jason") || name.equals("Matthew"))) {
				throw new BankException("Account name not recognized");
			}
			
			in = new BufferedReader(new FileReader((name + ".acct")));

			s_pin = in.readLine().split("\\s+");
			s_bal = in.readLine().split("\\s+");
			
			in.close();
			
			if (s_bal != null && s_pin != null) {
				
				float bal = this.<Float>parseLine(s_bal, "bal");
				int pin = this.<Integer>parseLine(s_pin, "pin");
								
				return new Account(bal, pin, name);
				
			} else {
				throw new BankException("Error: Account file formatted incorrectly");
			}
						
		} catch (IOException e) {
			throw new BankException("Error: Account file not found");
		}
	}
	
	
	private void parseCommand(String[] cmd, Account acct) throws BankException {
		switch (cmd[0]) {
			case "getBalance":
				getBal(cmd, acct);
				break;
			case "deposit":
				deposit(cmd, acct);
				break;
			case "withdraw":
				withdraw(cmd, acct);
				break;
			default:
				throw new BankException("Command not recognized. Recognized commands are \"getBalance\", \"deposit\", and \"withdraw\"");
		}
		updateAccount(acct);
	}
	
	private void updateAccount(Account acct) throws BankException {
		
		BufferedWriter out = null;
		String s_bal[];
		String s_pin[];
		
		try {
			//prevent them from opening any other potentially malicious files
			if (!(acct.getName().equals("Jason") || acct.getName().equals("Matthew"))) {
				throw new BankException("Account name not recognized");
			}
			
			out = new BufferedWriter(new FileWriter((acct.getName() + ".acct")));
			
			out.write("pin: " + String.valueOf(acct.getPin()));
			out.newLine();
			out.flush();
			out.write("bal: " + String.valueOf(acct.getBal()));
			out.flush();
			
			out.close();
			
		} catch (IOException e) {
			throw new BankException("Error: Account file not found");
		}
	}
	
	private void getBal(String cmd[], Account acct) throws BankException {
		checkPin(acct);
		write(String.valueOf(acct.getBal()));
	}
	
	private void deposit(String cmd[], Account acct) throws BankException {
		if (dir) {
			if (cmd.length == 3) {
				float amt = parseAmt(cmd[2]);
				acct.deposit(amt);
				write(String.valueOf(acct.getBal()));
			} else {
				throw new BankException("Invalid number of arguments. Two arguments (account, amount) are used by this command.");
			}
		} else {
			throw new BankException("Deposits can only be made via a direct connection.");
		}
	}
	
	private void withdraw(String cmd[], Account acct) throws BankException {
		if (cmd.length == 3) {
			float amt = parseAmt(cmd[2]);
			checkPin(acct);
			acct.withdraw(amt);
			write(String.valueOf(acct.getBal()));
			
		} else {
			throw new BankException("Invalid number of arguments. Two arguments (account, amount) are used by this command.");
		}
	}
	
	private float parseAmt(String amt) throws BankException {
		try {
			return Float.parseFloat(amt);
		} catch (NumberFormatException e){
			throw new BankException("Invalid amount; enter only integers");
		}
	}
	
	private boolean checkPin(Account acct) throws BankException {
		String in;
		int pin_in;
		//TODO: add limit on PIN guesses. When reached, return false; failed PIN
		while (true) {
			write("pin?");
			in = read();
			if (in.length() != 4) {
				throw new BankException("Invalid Format: Enter a four digit (0-9) integer");
			}
			try {
				pin_in = Integer.parseInt(in);
				if (pin_in == acct.getPin()) {
					return true;
				} else {
					write("Invalid PIN");
				}
			} catch (NumberFormatException e) {
				throw new BankException("Invalid Format: Enter a four digit (0-9) integer");
			}
		}
	}
	
	//TODO: handle this better
	private void write(String msg) {
				
		try {
			bw.write(msg);
			bw.newLine();
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//TODO: handle this better
	private String read() {
		try {
			String in = br.readLine();			
			return in;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}