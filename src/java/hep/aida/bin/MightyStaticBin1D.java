package hep.aida.bin;

import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;
/**
 * Static and the same as its superclass, except that it can do more: Additionally computes moments of arbitrary integer order, harmonic mean, geometric mean, etc.
 * 
 * Constructors need to be told what functionality is required for the given use case.
 * Only maintains aggregate measures (incrementally) - the added elements themselves are not kept.
 * 
 * @author wolfgang.hoschek@cern.ch
 * @version 0.9, 03-Jul-99
 */
public class MightyStaticBin1D extends StaticBin1D {
	protected boolean hasSumOfLogarithms = false;
	protected double sumOfLogarithms = 0.0; // Sum( Log(x[i]) )

	protected boolean hasSumOfInversions = false;
	protected double sumOfInversions = 0.0; // Sum( 1/x[i] )

	protected double[] sumOfPowers = null;  // Sum( x[i]^3 ) .. Sum( x[i]^max_k )
/**
 * Constructs and returns an empty bin with limited functionality but good performance; equivalent to <tt>MightyStaticBin1D(false,false,4)</tt>.
 */
public MightyStaticBin1D() {
	this(false, false, 4);
}
/**
 * Constructs and returns an empty bin with the given capabilities.
 *
 * @param hasSumOfLogarithms  Tells whether {@link #sumOfLogarithms()} can return meaningful results.
 *        Set this parameter to <tt>false</tt> if measures of sum of logarithms, geometric mean and product are not required.
 * <p>
 * @param hasSumOfInversions  Tells whether {@link #sumOfInversions()} can return meaningful results.
 *        Set this parameter to <tt>false</tt> if measures of sum of inversions, harmonic mean and sumOfPowers(-1) are not required.
 * <p>
 * @param maxOrderForSumOfPowers  The maximum order <tt>k</tt> for which {@link #sumOfPowers(int)} can return meaningful results.
 *        Set this parameter to at least 3 if the skew is required, to at least 4 if the kurtosis is required.
 *        In general, if moments are required set this parameter at least as large as the largest required moment.
 *        This method always substitutes <tt>Math.max(2,maxOrderForSumOfPowers)</tt> for the parameter passed in.
 *        Thus, <tt>sumOfPowers(0..2)</tt> always returns meaningful results.
 *
 * @see #hasSumOfPowers(int)
 * @see #moment(int,double)
 */
public MightyStaticBin1D(boolean hasSumOfLogarithms, boolean hasSumOfInversions, int maxOrderForSumOfPowers) {
	setMaxOrderForSumOfPowers(maxOrderForSumOfPowers);
	this.hasSumOfLogarithms = hasSumOfLogarithms;
	this.hasSumOfInversions = hasSumOfInversions;
	this.clear();
}
/**
 * Adds the part of the specified list between indexes <tt>from</tt> (inclusive) and <tt>to</tt> (inclusive) to the receiver.
 *
 * @param list the list of which elements shall be added.
 * @param from the index of the first element to be added (inclusive).
 * @param to the index of the last element to be added (inclusive).
 * @throws IndexOutOfBoundsException if <tt>list.size()&gt;0 && (from&lt;0 || from&gt;to || to&gt;=list.size())</tt>.
 */
public synchronized void addAllOfFromTo(DoubleArrayList list, int from, int to) {
	super.addAllOfFromTo(list, from, to);
	
	if (this.sumOfPowers != null) {
		//int max_k = this.min_k + this.sumOfPowers.length-1;
		Descriptive.incrementalUpdateSumsOfPowers(list, from, to, 3, getMaxOrderForSumOfPowers(), this.sumOfPowers);
	}

	if (this.hasSumOfInversions) {
		this.sumOfInversions += Descriptive.sumOfInversions(list, from, to);
	}
	
	if (this.hasSumOfLogarithms) {
		this.sumOfLogarithms += Descriptive.sumOfLogarithms(list, from, to);
	}
}
/**
 * Resets the values of all measures.
 */
protected void clearAllMeasures() {
	super.clearAllMeasures();
	
	this.sumOfLogarithms = 0.0;
	this.sumOfInversions = 0.0;
	
	if (this.sumOfPowers != null) {
		for (int i=this.sumOfPowers.length; --i >=0; ) {
			this.sumOfPowers[i] = 0.0;
		}
	}
}
/**
 * Returns a deep copy of the receiver.
 *
 * @return a deep copy of the receiver.
 */
public synchronized Object clone() {
	MightyStaticBin1D clone = (MightyStaticBin1D) super.clone();
	if (this.sumOfPowers != null) clone.sumOfPowers = (double[]) clone.sumOfPowers.clone();
	return clone;
}
/**
 * Computes the deviations from the receiver's measures to another bin's measures.
 * @param other the other bin to compare with
 * @return a summary of the deviations.
 */
public String compareWith(AbstractBin1D other) {
	StringBuffer buf = new StringBuffer(super.compareWith(other));
	if (other instanceof MightyStaticBin1D) {
		MightyStaticBin1D m = (MightyStaticBin1D) other;
		if (hasSumOfLogarithms() && m.hasSumOfLogarithms())
			buf.append("geometric mean: "+relError(geometricMean(),m.geometricMean()) +" %\n");
		if (hasSumOfInversions() && m.hasSumOfInversions())
			buf.append("harmonic mean: "+relError(harmonicMean(),m.harmonicMean()) +" %\n");
		if (hasSumOfPowers(3) && m.hasSumOfPowers(3))
			buf.append("skew: "+relError(skew(),m.skew()) +" %\n");
		if (hasSumOfPowers(4) && m.hasSumOfPowers(4))
			buf.append("kurtosis: "+relError(kurtosis(),m.kurtosis()) +" %\n");
		buf.append("\n");
	}
	return buf.toString();
}
/**
 * Returns the geometric mean, which is <tt>Product( x[i] )<sup>1.0/size()</sup></tt>.
 *
 * This method tries to avoid overflows at the expense of an equivalent but somewhat inefficient definition:
 * <tt>geoMean = exp( Sum( Log(x[i]) ) / size())</tt>.
 * Note that for a geometric mean to be meaningful, the minimum of the data sequence must not be less or equal to zero.
 * @return the geometric mean; <tt>Double.NaN</tt> if <tt>!hasSumOfLogarithms()</tt>.
 */
public synchronized double geometricMean() {
	return Descriptive.geometricMean(size(), sumOfLogarithms());
}
/**
 * Returns the maximum order <tt>k</tt> for which sums of powers are retrievable, as specified upon instance construction.
 * @see #hasSumOfPowers(int)
 * @see #sumOfPowers(int)
 */
public synchronized int getMaxOrderForSumOfPowers() {
		/* order 0..2 is always recorded.
		   order 0 is size()
		   order 1 is sum()
		   order 2 is sum_xx()
		*/
	if (this.sumOfPowers == null) return 2;
	
	return 2 + this.sumOfPowers.length;
}
/**
 * Returns the minimum order <tt>k</tt> for which sums of powers are retrievable, as specified upon instance construction.
 * @see #hasSumOfPowers(int)
 * @see #sumOfPowers(int)
 */
public synchronized int getMinOrderForSumOfPowers() {
	int minOrder = 0;
	if (hasSumOfInversions()) minOrder = -1;
	return minOrder;
}
/**
 * Returns the harmonic mean, which is <tt>size() / Sum( 1/x[i] )</tt>.
 * Remember: If the receiver contains at least one element of <tt>0.0</tt>, the harmonic mean is <tt>0.0</tt>.
 * @return the harmonic mean; <tt>Double.NaN</tt> if <tt>!hasSumOfInversions()</tt>.
 * @see #hasSumOfInversions()
 */
public synchronized double harmonicMean() {
	return Descriptive.harmonicMean(size(), sumOfInversions());
}
/**
 * Returns whether <tt>sumOfInversions()</tt> can return meaningful results.
 * @return <tt>false</tt> if the bin was constructed with insufficient parametrization, <tt>true</tt> otherwise.
 * See the constructors for proper parametrization.
 */
public boolean hasSumOfInversions() {
	return this.hasSumOfInversions;
}
/**
 * Tells whether <tt>sumOfLogarithms()</tt> can return meaningful results.
 * @return <tt>false</tt> if the bin was constructed with insufficient parametrization, <tt>true</tt> otherwise.
 * See the constructors for proper parametrization.
 */
public boolean hasSumOfLogarithms() {
	return this.hasSumOfLogarithms;
}
/**
 * Tells whether <tt>sumOfPowers(k)</tt> can return meaningful results.
 * Defined as <tt>hasSumOfPowers(k) <==> getMinOrderForSumOfPowers() <= k && k <= getMaxOrderForSumOfPowers()</tt>.
 * A return value of <tt>true</tt> implies that <tt>hasSumOfPowers(k-1) .. hasSumOfPowers(0)</tt> will also return <tt>true</tt>.
 * See the constructors for proper parametrization.
 * <p>
 * <b>Details</b>: 
 * <tt>hasSumOfPowers(0..2)</tt> will always yield <tt>true</tt>.
 * <tt>hasSumOfPowers(-1) <==> hasSumOfInversions()</tt>.
 *
 * @return <tt>false</tt> if the bin was constructed with insufficient parametrization, <tt>true</tt> otherwise.
 * @see #getMinOrderForSumOfPowers()
 * @see #getMaxOrderForSumOfPowers()
 */
public boolean hasSumOfPowers(int k) {
	return getMinOrderForSumOfPowers() <= k && k <= getMaxOrderForSumOfPowers();
}
/**
 * Returns the kurtosis (aka excess), which is <tt>-3 + moment(4,mean()) / standardDeviation()<sup>4</sup></tt>.
 * @return the kurtosis; <tt>Double.NaN</tt> if <tt>!hasSumOfPowers(4)</tt>.
 * @see #hasSumOfPowers(int)
 */
public synchronized double kurtosis() {
	return Descriptive.kurtosis( moment(4,mean()), standardDeviation() );
}
/**
 * Returns the moment of <tt>k</tt>-th order with value <tt>c</tt>,
 * which is <tt>Sum( (x[i]-c)<sup>k</sup> ) / size()</tt>.
 *
 * @param k the order; must be greater than or equal to zero.
 * @param c any number.
 * @throws IllegalArgumentException if <tt>k < 0</tt>.
 * @return <tt>Double.NaN</tt> if <tt>!hasSumOfPower(k)</tt>.
 */
public synchronized double moment(int k, double c) {
	if (k<0) throw new IllegalArgumentException("k must be >= 0");
	//checkOrder(k);
	if (!hasSumOfPowers(k)) return Double.NaN;

	int maxOrder = Math.min(k,getMaxOrderForSumOfPowers());
	DoubleArrayList sumOfPows = new DoubleArrayList(maxOrder+1);
	sumOfPows.add(size());
	sumOfPows.add(sum());
	sumOfPows.add(sumOfSquares());
	for (int i=3; i<=maxOrder; i++) sumOfPows.add(sumOfPowers(i));
	
	return Descriptive.moment(k, c, size(), sumOfPows.elements());
}
/**
 * Returns the product, which is <tt>Prod( x[i] )</tt>.
 * In other words: <tt>x[0]*x[1]*...*x[size()-1]</tt>.
 * @return the product; <tt>Double.NaN</tt> if <tt>!hasSumOfLogarithms()</tt>.
 * @see #hasSumOfLogarithms()
 */
public double product() {
	return Descriptive.product(size(), sumOfLogarithms());
}
/**
 * Sets the range of orders in which sums of powers are to be computed.
 * In other words, <tt>sumOfPower(k)</tt> will return <tt>Sum( x[i]^k )</tt> if <tt>min_k <= k <= max_k || 0 <= k <= 2</tt>
 * and throw an exception otherwise.
 * @see #isLegalOrder(int)
 * @see #sumOfPowers(int)
 * @see #getRangeForSumOfPowers()
 */
protected void setMaxOrderForSumOfPowers(int max_k) {
	//if (max_k < ) throw new IllegalArgumentException();
	
	if (max_k <=2) {
		this.sumOfPowers = null;
	}
	else {
		this.sumOfPowers = new double[max_k - 2];
	}
}
/**
 * Returns the skew, which is <tt>moment(3,mean()) / standardDeviation()<sup>3</sup></tt>.
 * @return the skew; <tt>Double.NaN</tt> if <tt>!hasSumOfPowers(3)</tt>.
 * @see #hasSumOfPowers(int)
 */
public synchronized double skew() {
	return Descriptive.skew( moment(3,mean()), standardDeviation() );
}
/**
 * Returns the sum of inversions, which is <tt>Sum( 1 / x[i] )</tt>.
 * @return the sum of inversions; <tt>Double.NaN</tt> if <tt>!hasSumOfInversions()</tt>.
 * @see #hasSumOfInversions()
 */
public double sumOfInversions() {
	if (! this.hasSumOfInversions) return Double.NaN;
	//if (! this.hasSumOfInversions) throw new IllegalOperationException("You must specify upon instance construction that the sum of inversions shall be computed.");
	return this.sumOfInversions;
}
/**
 * Returns the sum of logarithms, which is <tt>Sum( Log(x[i]) )</tt>.
 * @return the sum of logarithms; <tt>Double.NaN</tt> if <tt>!hasSumOfLogarithms()</tt>.
 * @see #hasSumOfLogarithms()
 */
public synchronized double sumOfLogarithms() {
	if (! this.hasSumOfLogarithms) return Double.NaN;
	//if (! this.hasSumOfLogarithms) throw new IllegalOperationException("You must specify upon instance construction that the sum of logarithms shall be computed.");
	return this.sumOfLogarithms;
}
/**
 * Returns the <tt>k-th</tt> order sum of powers, which is <tt>Sum( x[i]<sup>k</sup> )</tt>.
 * @param k the order of the powers.
 * @return the sum of powers; <tt>Double.NaN</tt> if <tt>!hasSumOfPowers(k)</tt>.
 * @see #hasSumOfPowers(int)
 */
public synchronized double sumOfPowers(int k) {
	if (!hasSumOfPowers(k)) return Double.NaN;
	//checkOrder(k);	
	if (k == -1) return sumOfInversions();
	if (k == 0) return size();
	if (k == 1) return sum();
	if (k == 2) return sumOfSquares();

	return this.sumOfPowers[k-3];
}
/**
 * Returns a String representation of the receiver.
 */
public synchronized String toString() {
	StringBuffer buf = new StringBuffer(super.toString());
	
	if (hasSumOfLogarithms()) {
		buf.append("Geometric mean: "+geometricMean());
		buf.append("\nProduct: "+product()+"\n");
	}
	
	if (hasSumOfInversions()) {
		buf.append("Harmonic mean: "+harmonicMean());
		buf.append("\nSum of inversions: "+sumOfInversions()+"\n");
	}

	int maxOrder = getMaxOrderForSumOfPowers();
	int maxPrintOrder = Math.min(6,maxOrder); // don't print tons of measures
	if (maxOrder>2) {
		if (maxOrder>=3) {
			buf.append("Skew: "+skew()+"\n");
		}
		if (maxOrder>=4) {
			buf.append("Kurtosis: "+kurtosis()+"\n");
		}
		for (int i=3; i<=maxPrintOrder; i++) {
			buf.append("Sum of powers("+i+"): "+sumOfPowers(i)+"\n");
		}
		for (int k=0; k<=maxPrintOrder; k++) {
			buf.append("Moment("+k+",0): "+moment(k,0)+"\n");
		}
		for (int k=0; k<=maxPrintOrder; k++) {
			buf.append("Moment("+k+",mean()): "+moment(k,mean())+"\n");
		}
	}
	return buf.toString();
}
/**
 * @throws IllegalOperationException if <tt>! isLegalOrder(k)</tt>.
 */
protected void xcheckOrder(int k) {
	//if (! isLegalOrder(k)) return Double.NaN;
	//if (! xisLegalOrder(k)) throw new IllegalOperationException("Illegal order of sum of powers: k="+k+". Upon instance construction legal range was fixed to be "+getMinOrderForSumOfPowers()+" <= k <= "+getMaxOrderForSumOfPowers());
}
/**
 * Returns whether two bins are equal; 
 * They are equal if the other object is of the same class or a subclass of this class and both have the same size, minimum, maximum, sum, sumOfSquares, sumOfInversions and sumOfLogarithms.
 */
protected boolean xequals(Object object) {
	if (!(object instanceof MightyStaticBin1D)) return false;
	MightyStaticBin1D other = (MightyStaticBin1D) object;
	return super.equals(other) && sumOfInversions()==other.sumOfInversions() && sumOfLogarithms()==other.sumOfLogarithms();
}
/**
 * Tells whether <tt>sumOfPowers(fromK) .. sumOfPowers(toK)</tt> can return meaningful results.
 * @return <tt>false</tt> if the bin was constructed with insufficient parametrization, <tt>true</tt> otherwise.
 * See the constructors for proper parametrization.
 * @throws IllegalArgumentException if <tt>fromK > toK</tt>.
 */
protected boolean xhasSumOfPowers(int fromK, int toK) {
	if (fromK > toK) throw new IllegalArgumentException("fromK must be less or equal to toK");
	return getMinOrderForSumOfPowers() <= fromK && toK <= getMaxOrderForSumOfPowers();
}
/**
 * Returns <tt>getMinOrderForSumOfPowers() <= k && k <= getMaxOrderForSumOfPowers()</tt>.
 */
protected synchronized boolean xisLegalOrder(int k) {
	return getMinOrderForSumOfPowers() <= k && k <= getMaxOrderForSumOfPowers();
}
}
