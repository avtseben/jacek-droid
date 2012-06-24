package pl.looksok.logic;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import pl.looksok.utils.FormatterHelper;
import pl.looksok.utils.exceptions.PaysNotCalculatedException;


public class PersonData implements Serializable{

	private static final long serialVersionUID = 4909331903428866567L;
	private String name;
	private HashSet<String> emails = new HashSet<String>();
	private double howMuchIPaid;
	private double howMuchIShouldPay;
	private double toReturn;
	private double totalRefundForThisPerson;
	private HashMap<String, PersonData> otherPeoplePayments;
	private HashMap<String, Double> refundForOtherPeople;
	private double alreadyReturned;
	private double alreadyRefunded = 0.0;
	
	public PersonData(String _personName, HashMap<String, PersonData> inputPays) {
		PersonData pd = inputPays.get(_personName);
		setPersonName(pd.getName());
		setPayMadeByPerson(pd.getPayMadeByPerson());
		setEmails(pd.getEmails());
		
		otherPeoplePayments = inputPays;
		refundForOtherPeople = new HashMap<String, Double>();
	}

	public PersonData(String name, double payDouble, HashSet<String> emails) {
		this.name = name;
		this.setPayMadeByPerson(payDouble);
		this.emails = emails;
	}

	public PersonData(String name, double payDouble, double shouldPayDouble, HashSet<String> emails) {
		this(name, payDouble, emails);
		setHowMuchPersonShouldPay(shouldPayDouble);
	}

	public String getName() {
		return name;
	}

	public void setPersonName(String personName) {
		this.name = personName;
	}

	public double getPayMadeByPerson() {
		return howMuchIPaid;
	}

	public void setPayMadeByPerson(Double payMadeByPerson) {
		this.howMuchIPaid = payMadeByPerson;
	}
	
	
	private boolean personBNeedsMoreThanIShouldGive(double tmpToReturn) {
		return alreadyReturned + tmpToReturn > toReturn;
	}
	
	public double getHowMuchPersonShouldPay() {
		return howMuchIShouldPay;
	}
	
	public void setHowMuchPersonShouldPay(double value) {
		this.howMuchIShouldPay = value;
	}
	
	public HashMap<String, Double> getRefundForOtherPeople() {
		return refundForOtherPeople;
	}
	
	public void setRefundForOtherPeople(HashMap<String, Double> refundForOtherPeople) {
		this.refundForOtherPeople = refundForOtherPeople;
	}
	
	public double getCalculatedReturnForPersonB(String personB) {
		return getRefundForOtherPeople().get(personB);
	}
	
	public double getAlreadyRefunded() {
		return alreadyRefunded;
	}
	
	public void setAlreadyRefunded(double alreadyRefunded) {
		this.alreadyRefunded = alreadyRefunded;
	}

	public void prepareCalculationData(double _howMuchPerPerson) {
		howMuchIShouldPay = FormatterHelper.roundDouble(_howMuchPerPerson, 2);
		
		calculateHowMuchIShouldReturn();
		calculateHowMuchRefundIShouldHave();
	}

	public void calculateRefundToOthers(Hashtable<String, PersonData> calculationResult) {
		Set<String> c = calculationResult.keySet();
		Iterator<String> it = c.iterator();
		
		while(it.hasNext()) {
			PersonData pp2 = calculationResult.get(it.next());
			if(this.getName() != pp2.getName()){
				double returnValue = howMuchIGiveBackToPersonB(pp2);
				getRefundForOtherPeople().put(pp2.getName(), returnValue);
			}
		}
	}
	
	private double howMuchIGiveBackToPersonB(PersonData personB) {
		try{
			double result = this.howMuchShouldReturnTo(personB.getName());
			return FormatterHelper.roundDouble(result, 2);
		}catch(NullPointerException e){
			throw new PaysNotCalculatedException("Call 'calculate' method before reading results");
		}
	}

	private void calculateHowMuchIShouldReturn() {
		toReturn = howMuchIShouldPay - howMuchIPaid;
		if(toReturn < 0.0)
			toReturn = 0.0;
	}

	private void calculateHowMuchRefundIShouldHave() {
		totalRefundForThisPerson = howMuchIPaid - howMuchIShouldPay;
		if(totalRefundForThisPerson < 0.0)
			totalRefundForThisPerson = 0.0;
	}

	public double getToReturn() {
		return toReturn;
	}

	public double getTotalRefundForThisPerson() {
		return totalRefundForThisPerson;
	}
	
	private double howMuchShouldReturnTo(String personBName){
		PersonData personBData = otherPeoplePayments.get(personBName);
		double howMuchPersonBPaid = personBData.getPayMadeByPerson();
		double howMuchPersonBShouldPay = personBData.getHowMuchPersonShouldPay();
		double howMuchRefundPersonBNeeds = howMuchPersonBPaid - howMuchPersonBShouldPay - personBData.getAlreadyRefunded();
		
		if(howMuchRefundPersonBNeeds>0){
			double tmpToReturn = howMuchIShouldPay - howMuchIPaid;
			
			if(personBNeedsMoreThanIShouldGive(tmpToReturn)){
				tmpToReturn = splitMyReturnAmount();
			}else if(personBWantsLessThanIHaveToReturn(howMuchPersonBPaid, howMuchPersonBShouldPay, tmpToReturn, howMuchRefundPersonBNeeds)){
				tmpToReturn = givePersonBNoMoreThanHeWants(howMuchPersonBPaid, howMuchPersonBShouldPay);
			}
			
			if(tmpToReturn > howMuchRefundPersonBNeeds){
				tmpToReturn = howMuchRefundPersonBNeeds;
			}
			
			if(tmpToReturn<0.0)
				tmpToReturn = 0.0;
			
			alreadyReturned += tmpToReturn;
			otherPeoplePayments.get(personBName).addToAlreadyRefunded(tmpToReturn);
			return tmpToReturn;
		}else
			return 0.0;
	}

	private void addToAlreadyRefunded(double valueToAdd) {
		setAlreadyRefunded(getAlreadyRefunded() + valueToAdd);
	}

	private double givePersonBNoMoreThanHeWants(double howMuchPersonBPaid, double howMuchPersonBShouldPay) {
		double tmpToReturn;
		double refundForPersonBNeeded = howMuchPersonBPaid - howMuchPersonBShouldPay;
		if(refundForPersonBNeeded<0)
			tmpToReturn = 0.0;
		
		tmpToReturn = howMuchPersonBPaid - howMuchIShouldPay;
		if(tmpToReturn<0)
			tmpToReturn = 0;
		return tmpToReturn;
	}

	private boolean personBWantsLessThanIHaveToReturn(
			double howMuchPersonBPaid, double howMuchPersonBShouldPay, double tmpToReturn, double howMuchRefundPersonBNeeds) {
		return howMuchRefundPersonBNeeds < howMuchIShouldPay - howMuchIPaid;
	}

	private double splitMyReturnAmount() {
		double tmpToReturn;
		tmpToReturn = toReturn - alreadyReturned;
		return tmpToReturn;
	}

	public String printPersonReturnsToOthers(){
		StringBuilder sb = new StringBuilder(getName());
		sb.append(" should return to:\n");
		
		Set<String> c = getRefundForOtherPeople().keySet();
		Iterator<String> it = c.iterator();
		
		while(it.hasNext()) {
			String key = it.next();
			double result = getRefundForOtherPeople().get(key);
			if(result >0)
			sb.append(key).append(": ").append(result).append("\n");
		}
		
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append(" paid: ").append(getPayMadeByPerson());
		
		return sb.toString();
	}

	public HashSet<String> getEmails() {
		return emails;
	}

	public void setEmails(HashSet<String> emails) {
		this.emails = emails;
	}
}