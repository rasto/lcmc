package hep.aida.bin;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.jet.random.engine.RandomEngine;
import cern.jet.stat.Descriptive;
/**
 * 1-dimensional rebinnable bin holding <tt>double</tt> elements;
 * Efficiently computes advanced statistics of data sequences.
 * Technically speaking, a multiset (or bag) with efficient statistics operations defined upon.
 * First see the <a href="package-summary.html">package summary</a> and javadoc <a href="package-tree.html">tree view</a> to get the broad picture.
 * <p>
 * The data filled into a <tt>DynamicBin1D</tt> is internally preserved in the bin. 
 * As a consequence this bin can compute more than only basic statistics.
 * On the other hand side, if you add huge amounts of elements, you may run out of memory (each element takes 8 bytes).
 * If this drawbacks matter, consider to use {@link StaticBin1D}, 
 * which overcomes them at the expense of limited functionality.
 * <p>
 * This class is fully thread safe (all public methods are synchronized).
 * Thus, you can have one or more threads adding to the bin as well as one or more threads reading and viewing the statistics of the bin <i>while it is filled</i>.
 * For high performance, add data in large chunks (buffers) via method <tt>addAllOf</tt> rather than piecewise via method <tt>add</tt>.
 * <p>
 * If your favourite statistics measure is not directly provided by this class, 
 * check out {@link cern.jet.stat.Descriptive} in combination with methods {@link #elements()} and {@link #sortedElements()}. 
 * <p>
 * <b>Implementation</b>:
 * Lazy evaluation, caching, incremental maintainance.
 * 
 * @see cern.jet.stat.Descriptive
 * @author wolfgang.hoschek@cern.ch
 * @version 0.9, 03-Jul-99
 */
public class DynamicBin1D extends QuantileBin1D {
	// Never ever use "this.size" as it would be intuitive!
	// This class abuses "this.size". "this.size" DOES NOT REFLECT the number of elements contained in the receiver!
	// Instead, "this.size" reflects the number of elements incremental stats computation has already processed.

	/**
	 * The elements contained in this bin.
	 */
	protected DoubleArrayList elements = null;

	/**
	 * The elements contained in this bin, sorted ascending.
	 */
	protected DoubleArrayList sortedElements = null;

	/**
	 * Preserve element order under all circumstances?
	 */
	protected boolean fixedOrder = false;

	// cached parameters
	//protected double skew = 0.0; 
	//protected double kurtosis = 0.0; 

	
	// cache states
	protected boolean isSorted = true;
	protected boolean isIncrementalStatValid = true;
	//protected boolean isSkewValid = true;
	//protected boolean isKurtosisValid = true;
	
	protected boolean isSumOfInversionsValid = true;
	protected boolean isSumOfLogarithmsValid = true;
	//protected boolean isSumOfPowersValid = true;

/**
 * Constructs and returns an empty bin; implicitly calls {@link #setFixedOrder(boolean) setFixedOrder(false)}.
 */
public DynamicBin1D() {
	super();
	this.clear();
	this.elements = new DoubleArrayList();
	this.sortedElements = new DoubleArrayList(0);
	this.fixedOrder = false;
	this.hasSumOfLogarithms = true;
	this.hasSumOfInversions = true;
}
/**
 * Adds the specified element to the receiver.
 *
 * @param element element to be appended.
 */
public synchronized void add(double element) {
	elements.add(element);
	invalidateAll();
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
	this.elements.addAllOfFromTo(list, from, to);
	this.invalidateAll();
}
/**
Applies a function to each element and aggregates the results.
Returns a value <tt>v</tt> such that <tt>v==a(size())</tt> where <tt>a(i) == aggr( a(i-1), f(x(i)) )</tt> and terminators are <tt>a(1) == f(x(0)), a(0)==Double.NaN</tt>.
<p>
<b>Example:</b>
<pre>
cern.jet.math.Functions F = cern.jet.math.Functions.functions;
bin = 0 1 2 3 

// Sum( x[i]*x[i] ) 
bin.aggregate(F.plus,F.square);
--> 14
</pre>
For further examples, see the <a href="package-summary.html#FunctionObjects">package doc</a>.

@param aggr an aggregation function taking as first argument the current aggregation and as second argument the transformed current element.
@param f a function transforming the current element.
@return the aggregated measure.
@see cern.jet.math.Functions
*/
public synchronized double aggregate(cern.colt.function.DoubleDoubleFunction aggr, cern.colt.function.DoubleFunction f) {
	int s = size();
	if (s==0) return Double.NaN;
	double a = f.apply(elements.getQuick(s-1));
	for (int i=s-1; --i >= 0; ) {
		a = aggr.apply(a, f.apply(elements.getQuick(i)));
	}
	return a;
}
/**
 * Removes all elements from the receiver.
 * The receiver will be empty after this call returns.
 */
public synchronized void clear() {
	super.clear();

	if (this.elements != null) this.elements.clear();
	if (this.sortedElements != null) this.sortedElements.clear();
	
	this.validateAll();
}
/**
 * Resets the values of all measures.
 */
protected void clearAllMeasures() {
	super.clearAllMeasures();
	
	//this.skew = 0.0;
	//this.kurtosis = 0.0;
}
/**
 * Returns a deep copy of the receiver.
 *
 * @return a deep copy of the receiver.
 */
public synchronized Object clone() {
	DynamicBin1D clone = (DynamicBin1D) super.clone();
	if (this.elements != null) clone.elements = clone.elements.copy();
	if (this.sortedElements != null) clone.sortedElements = clone.sortedElements.copy();
	return clone;
}
/**
 * Returns the correlation of two bins, which is <tt>corr(x,y) = covariance(x,y) / (stdDev(x)*stdDev(y))</tt> (Pearson's correlation coefficient).
 * A correlation coefficient varies between -1 (for a perfect negative relationship) to +1 (for a perfect positive relationship). 
 * See the <A HREF="http://www.cquest.utoronto.ca/geog/ggr270y/notes/not05efg.html"> math definition</A>
 * and <A HREF="http://www.stat.berkeley.edu/users/stark/SticiGui/Text/gloss.htm#correlation_coef"> another def</A>.
 * @param other the bin to compare with.
 * @return the correlation.
 * @throws IllegalArgumentException if <tt>size() != other.size()</tt>.
 */
public synchronized double correlation(DynamicBin1D other) {
	synchronized (other) {
		return covariance(other) / (standardDeviation()*other.standardDeviation());
	}
}
/**
 * Returns the covariance of two bins, which is <tt>cov(x,y) = (1/size()) * Sum((x[i]-mean(x)) * (y[i]-mean(y)))</tt>.
 * See the <A HREF="http://www.cquest.utoronto.ca/geog/ggr270y/notes/not05efg.html"> math definition</A>.
 * @param other the bin to compare with.
 * @return the covariance.
 * @throws IllegalArgumentException if <tt>size() != other.size()</tt>.
 */
public synchronized double covariance(DynamicBin1D other) {
	synchronized (other) {
		if (size() != other.size()) throw new IllegalArgumentException("both bins must have same size");
		double s = 0;
			for (int i=size(); --i >= 0; ) {
				s += this.elements.getQuick(i) * other.elements.getQuick(i);
			}
		
		double cov = (s - sum()*other.sum()/size()) / size();
		return cov;
	}
}
/**
 * Returns a copy of the currently stored elements.
 * Concerning the order in which elements are returned, see {@link #setFixedOrder(boolean)}.
 * @return a copy of the currently stored elements.
 */
public synchronized DoubleArrayList elements() {
	// safe since we are already synchronized.
	return elements_unsafe().copy();
}
/**
 * Returns the currently stored elements; <b>WARNING:</b> not a copy of them.
 * Thus, improper usage of the returned list may not only corrupt the receiver's internal state, but also break thread safety!
 * Only provided for performance and memory sensitive applications.
 * Do not modify the returned list unless you know exactly what you're doing.
 * This method can be used in a thread safe, clean <i>and</i> performant way by
 * explicitly synchronizing on the bin, as follows:
 * <pre>
 * ...
 * double sinSum = 0;
 * synchronized (dynamicBin) { // lock out anybody else
 *     DoubleArrayList elements = dynamicBin.elements_unsafe();
 *     // read each element and do something with it, for example
 * 	   double[] values = elements.elements(); // zero-copy
 * 	   for (int i=dynamicBin.size(); --i >=0; ) {
 *         sinSum += Math.sin(values[i]);
 * 	   }
 * }
 * System.out.println(sinSum);
 * ...
 * </pre>
 *
 * Concerning the order in which elements are returned, see {@link #setFixedOrder(boolean)}.
 * @return the currently stored elements.
 */
protected synchronized DoubleArrayList elements_unsafe() {
	return this.elements;
}
/**
 * Returns whether two bins are equal. 
 * They are equal if the other object is of the same class or a subclass of this class and both
 * have the same size, minimum, maximum, sum and sumOfSquares
 * and have the same elements, order being irrelevant (multiset equality).
 * <p>
 * Definition of <i>Equality</i> for multisets:
 * A,B are equal <=> A is a superset of B and B is a superset of A.
 * (Elements must occur the same number of times, order is irrelevant.)
 */
public synchronized boolean equals(Object object) {
	if (!(object instanceof DynamicBin1D)) return false;
	if (! super.equals(object)) return false;
	
	DynamicBin1D other = (DynamicBin1D) object;
	double[] s1 = sortedElements_unsafe().elements();
	synchronized (other) {
		double[] s2 = other.sortedElements_unsafe().elements();
		int n = size();
		return includes(s1,s2,0,n,0,n) &&
			includes(s2,s1,0,n,0,n);
	}
}
private static boolean includes(double[] array1, double[] array2, int first1, int last1,	int first2,	int last2) {
		while (first1 < last1 && first2 < last2) {
			if (array2[first2] < array1[first1])
				return false;
			else if (array1[first1] < array2[first2])
				++first1;
			else {
				++first1;
				++first2;
			}
		}

		return first2 == last2;
}

/**
 * Computes the frequency (number of occurances, count) of each distinct element.
 * After this call returns both <tt>distinctElements</tt> and <tt>frequencies</tt> have a new size (which is equal for both), which is the number of distinct elements currently contained.
 * <p>
 * Distinct elements are filled into <tt>distinctElements</tt>, starting at index 0.
 * The frequency of each distinct element is filled into <tt>frequencies</tt>, starting at index 0.
 * Further, both <tt>distinctElements</tt> and <tt>frequencies</tt> are sorted ascending by "element" (in sync, of course).
 * As a result, the smallest distinct element (and its frequency) can be found at index 0, the second smallest distinct element (and its frequency) at index 1, ..., the largest distinct element (and its frequency) at index <tt>distinctElements.size()-1</tt>.
 * <p>
 * <b>Example:</b>
 * <br>
 * <tt>elements = (8,7,6,6,7) --> distinctElements = (6,7,8), frequencies = (2,2,1)</tt>
 *
 * @param distinctElements a list to be filled with the distinct elements; can have any size.
 * @param frequencies      a list to be filled with the frequencies; can have any size; set this parameter to <tt>null</tt> to ignore it.
 */
public synchronized void frequencies(DoubleArrayList distinctElements, IntArrayList frequencies) {
	Descriptive.frequencies(sortedElements_unsafe(), distinctElements, frequencies);
}
/**
 * Returns a map holding the frequency distribution, that is, (distintElement,frequency) pairs.
 * The frequency (count) of an element is its number of occurances.
 * <p>
 * <b>Example:</b>
 * <br>
 * <tt>elements = (8,7,6,6,7) --> map.keys = (8,6,7), map.values = (1,2,2)</tt>
 *
 * @return a map holding the frequency distribution.
 */
private synchronized cern.colt.map.AbstractDoubleIntMap frequencyMap() {
	//cern.colt.map.OpenDoubleIntHashMap.hashCollisions = 0;
	// fill a map that collects frequencies
	cern.colt.map.AbstractDoubleIntMap map = new cern.colt.map.OpenDoubleIntHashMap();
	//cern.colt.Timer timer = new cern.colt.Timer().start();
	for (int i=size(); --i >= 0; ) {
		double element = this.elements.getQuick(i);
		//double element = i; // benchmark only TODO
		//double element = i%1000; // benchmark only TODO
		map.put(element,1 + map.get(element));
	}
	//timer.stop();
	//System.out.println("filling map took = "+timer);
	//System.out.println("collisions="+cern.colt.map.OpenDoubleIntHashMap.hashCollisions);
	
	return map;
}
/**
 * Returns <tt>Integer.MAX_VALUE</tt>, the maximum order <tt>k</tt> for which sums of powers are retrievable.
 * @see #hasSumOfPowers(int)
 * @see #sumOfPowers(int)
 */
public int getMaxOrderForSumOfPowers() {
	return Integer.MAX_VALUE;
}
/**
 * Returns <tt>Integer.MIN_VALUE</tt>, the minimum order <tt>k</tt> for which sums of powers are retrievable.
 * @see #hasSumOfPowers(int)
 * @see #sumOfPowers(int)
 */
public int getMinOrderForSumOfPowers() {
	return Integer.MIN_VALUE;
}
/**
 * 
 *
 * @param element element to be appended.
 */
protected void invalidateAll() {
	this.isSorted = false;
	this.isIncrementalStatValid = false;
	
	//this.isSkewValid = false;
	//this.isKurtosisValid = false;
	
	this.isSumOfInversionsValid = false;
	this.isSumOfLogarithmsValid = false;
}
/**
 * Returns <tt>true</tt>.
 * Returns whether a client can obtain all elements added to the receiver.
 * In other words, tells whether the receiver internally preserves all added elements.
 * If the receiver is rebinnable, the elements can be obtained via <tt>elements()</tt> methods.
 *
 */
public synchronized boolean isRebinnable() {
	return true;
}
/**
 * Returns the maximum.
 */
public synchronized double max() {
	if (! isIncrementalStatValid) updateIncrementalStats();
	return this.max;
}
/**
 * Returns the minimum.
 */
public synchronized double min() {
	if (! isIncrementalStatValid) updateIncrementalStats();
	return this.min;
}
/**
 * Returns the moment of <tt>k</tt>-th order with value <tt>c</tt>,
 * which is <tt>Sum( (x[i]-c)<sup>k</sup> ) / size()</tt>.
 * @param k the order; any number - can be less than zero, zero or greater than zero.
 * @param c any number.
 */
public synchronized double moment(int k, double c) {
	// currently no caching for this parameter
	return Descriptive.moment(this.elements, k, c);
}
/**
 * Returns the exact <tt>phi-</tt>quantile; that is, the smallest contained element <tt>elem</tt> for which holds that <tt>phi</tt> percent of elements are less than <tt>elem</tt>.
 * @param phi must satisfy <tt>0 &lt; phi &lt; 1</tt>.
 */
public synchronized double quantile(double phi) {
	return Descriptive.quantile(sortedElements_unsafe(),phi);
}
/**
 * Returns exactly how many percent of the elements contained in the receiver are <tt>&lt;= element</tt>.
 * Does linear interpolation if the element is not contained but lies in between two contained elements.
 *
 * @param element the element to search for.
 * @return the exact percentage <tt>phi</tt> of elements <tt>&lt;= element</tt> (<tt>0.0 &lt;= phi &lt;= 1.0)</tt>.
 */
public synchronized double quantileInverse(double element) {
	return Descriptive.quantileInverse(sortedElements_unsafe(),element);
}
/**
 * Returns the exact quantiles of the specified percentages.
 * @param percentages the percentages for which quantiles are to be computed.
 * Each percentage must be in the interval <tt>(0.0,1.0]</tt>. <tt>percentages</tt> must be sorted ascending.
 * @return the exact quantiles.
 */
public DoubleArrayList quantiles(DoubleArrayList percentages) {
	return Descriptive.quantiles(sortedElements_unsafe(),percentages);
}
/**
 * Removes from the receiver all elements that are contained in the specified list.
 *
 * @param list the elements to be removed.
 * @return <code>true</code> if the receiver changed as a result of the call.
 */
public synchronized boolean removeAllOf(DoubleArrayList list) {
	boolean changed = this.elements.removeAll(list);
	if (changed) {
		clearAllMeasures();
		invalidateAll();
		this.size = 0;
		if (fixedOrder) {
			this.sortedElements.removeAll(list);
			this.isSorted = true;
		}
	}
	return changed;
}
/**
 * Uniformly samples (chooses) <tt>n</tt> random elements <i>with or without replacement</i> from the contained elements and adds them to the given buffer.
 * If the buffer is connected to a bin, the effect is that the chosen elements are added to the bin connected to the buffer.
 * Also see {@link #buffered(int) buffered}.
 *
 * @param n the number of elements to choose.
 * @param withReplacement <tt>true</tt> samples with replacement, otherwise samples without replacement.
 * @param randomGenerator a random number generator. Set this parameter to <tt>null</tt> to use a default random number generator seeded with the current time.
 * @param buffer the buffer to which chosen elements will be added.
 * @throws IllegalArgumentException if <tt>!withReplacement && n > size()</tt>.
 * @see cern.jet.random.sampling
 */
public synchronized void sample(int n, boolean withReplacement, RandomEngine randomGenerator, cern.colt.buffer.DoubleBuffer buffer) {
	if (randomGenerator==null) randomGenerator = cern.jet.random.Uniform.makeDefaultGenerator();
	buffer.clear();
	
	if (!withReplacement) { // without
		if (n>size()) throw new IllegalArgumentException("n must be less than or equal to size()");
		cern.jet.random.sampling.RandomSamplingAssistant sampler = new cern.jet.random.sampling.RandomSamplingAssistant(n,size(),randomGenerator);
		for (int i=n; --i >= 0; ) {
			if (sampler.sampleNextElement()) buffer.add(this.elements.getQuick(i));
		}
	}
	else { // with
		cern.jet.random.Uniform uniform = new cern.jet.random.Uniform(randomGenerator);
		int s = size();
		for (int i=n; --i >= 0; ) {
			buffer.add(this.elements.getQuick(uniform.nextIntFromTo(0,s-1)));
		}
	buffer.flush();
	}
}
/**
Generic bootstrap resampling. 
Quite optimized - Don't be afraid to try it. 
Executes <tt>resamples</tt> resampling steps. In each resampling step does the following: 
<ul>
  <li>Uniformly samples (chooses) <tt>size()</tt> random elements <i>with replacement</i> 
	from <tt>this</tt> and fills them into an auxiliary bin <tt>b1</tt>. 
  <li>Uniformly samples (chooses) <tt>other.size()</tt> random elements <i>with replacement</i> 
	from <tt>other</tt> and fills them into another auxiliary bin <tt>b2</tt>. 
  <li>Executes the comparison function <tt>function</tt> on both auxiliary bins 
	(<tt>function.apply(b1,b2)</tt>) and adds the result of the function to an auxiliary bootstrap bin <tt>b3</tt>. 
</ul>
<p>Finally returns the auxiliary bootstrap bin <tt>b3</tt> from which the measure of interest 
  can be read off. </p>
<p><b>Background:</b></p>
<p>Also see a more <A HREF="http://garnet.acns.fsu.edu/~pkelly/bootstrap.html"> in-depth discussion</A> on bootstrapping and related randomization methods.
The classical statistical test for comparing the means of two samples is the 
  <i>t-test</i>. Unfortunately, this test assumes that the two samples each come 
  from a normal distribution and that these distributions have the same standard 
  deviation. Quite often, however, data has a distribution that is non-normal 
  in many ways. In particular, distributions are often unsymmetric. For such data, 
  the t-test may produce misleading results and should thus not be used. Sometimes 
  asymmetric data can be transformed into normally distributed data by taking 
  e.g. the logarithm and the t-test will then produce valid results, but this 
  still requires postulation of a certain distribution underlying the data, which 
  is often not warranted, because too little is known about the data composition.</p>
<p><i>Bootstrap resampling of means differences</i> (and other differences) is 
  a robust replacement for the t-test and does not require assumptions about the actual 
  distribution of the data. The idea of bootstrapping is quite simple: simulation. 
  The only assumption required is that the two samples <tt>a</tt> and <tt>b</tt> 
  are representative for the underlying distribution with respect to the statistic 
  that is being tested - this assumption is of course implicit in all statistical 
  tests. We can now generate lots of further samples that correspond to the two 
  given ones, by sampling <i>with replacement</i>. This process is called <i>resampling</i>. 
  A resample can (and usually will) have a different mean than the original one 
  and by drawing hundreds or thousands of such resamples <tt>a<sub>r</sub></tt> 
  from <tt>a</tt> and <tt>b<sub>r</sub></tt> from <tt>b</tt> we can compute the 
  so-called bootstrap distribution of all the differences &quot;mean of <tt>a<sub>r</sub></tt> 
  minus mean of <tt>b<sub>r</sub></tt>&quot;. That is, a bootstrap bin filled with the differences. Now we 
  can compute, what fraction of these differences is, say, greater than zero. 
  Let's assume we have computed 1000 resamples of both <tt>a</tt> and <tt>b</tt> 
  and found that only <tt>8</tt> of the differences were greater than zero. Then <tt>8/1000</tt> 
  or <tt>0.008</tt> is the p-value (probability) for the hypothesis that the mean 
  of the distribution underlying <tt>a</tt> is actually larger than the mean of 
  the distribution underlying <tt>b</tt>. From this bootstrap test, we can clearly 
  reject the hypothesis.</p>
<p>Instead of using means differences, we can also use other differences, for 
  example, the median differences.</p>
<p>Instead of p-values we can also read arbitrary confidence intervals from the 
  bootstrap bin. For example, <tt>90%</tt> of all bootstrap differences 
  are left of the value <tt>-3.5</tt>, hence a left <tt>90%</tt> confidence interval 
  for the difference would be <tt>(3.5,infinity)</tt>; in other words: the difference 
  is <tt>3.5</tt> or larger with probability <tt>0.9</tt>.</p>
<p>Sometimes we would like to compare not only means and medians, but also the 
  variability (spread) of two samples. The conventional method of doing this is 
  the <i>F-test</i>, which compares the standard deviations. It is related to 
  the t-test and, like the latter, assumes the two samples to come from a normal 
  distribution. The F-test is very sensitive to data with deviations from normality. 
  Instead we can again resort to more robust bootstrap resampling and compare a measure of 
  spread, for example the inter-quartile range. This way we compute a <i>bootstrap 
  resampling of inter-quartile range differences</i> in order to arrive at a test 
  for inequality or variability.
</p>
<p> 
<b>Example:</b> 
<table>
<td class="PRE"> 
<pre>
// v1,v2 - the two samples to compare against each other
double[] v1 = { 1, 2, 3, 4, 5, 6, 7, 8, 9,10,  21,  22,23,24,25,26,27,28,29,30,31};
double[] v2 = {10,11,12,13,14,15,16,17,18,19,  20,  30,31,32,33,34,35,36,37,38,39};
hep.aida.bin.DynamicBin1D X = new hep.aida.bin.DynamicBin1D();
hep.aida.bin.DynamicBin1D Y = new hep.aida.bin.DynamicBin1D();
X.addAllOf(new cern.colt.list.DoubleArrayList(v1));
Y.addAllOf(new cern.colt.list.DoubleArrayList(v2));
cern.jet.random.engine.RandomEngine random = new cern.jet.random.engine.MersenneTwister();

// bootstrap resampling of differences of means:
BinBinFunction1D diff = new BinBinFunction1D() {
&nbsp;&nbsp;&nbsp;public double apply(DynamicBin1D x, DynamicBin1D y) {return x.mean() - y.mean();}
};

// bootstrap resampling of differences of medians:
BinBinFunction1D diff = new BinBinFunction1D() {
&nbsp;&nbsp;&nbsp;public double apply(DynamicBin1D x, DynamicBin1D y) {return x.median() - y.median();}
};

// bootstrap resampling of differences of inter-quartile ranges:
BinBinFunction1D diff = new BinBinFunction1D() {
&nbsp;&nbsp;&nbsp;public double apply(DynamicBin1D x, DynamicBin1D y) {return (x.quantile(0.75)-x.quantile(0.25)) - (y.quantile(0.75)-y.quantile(0.25)); }
};

DynamicBin1D boot = X.sampleBootstrap(Y,1000,random,diff);

cern.jet.math.Functions F = cern.jet.math.Functions.functions;
System.out.println("p-value="+ (boot.aggregate(F.plus, F.greater(0)) / boot.size()));
System.out.println("left 90% confidence interval = ("+boot.quantile(0.9) + ",infinity)");

-->
// bootstrap resampling of differences of means:
p-value=0.0080
left 90% confidence interval = (-3.571428571428573,infinity)

// bootstrap resampling of differences of medians:
p-value=0.36
left 90% confidence interval = (5.0,infinity)

// bootstrap resampling of differences of inter-quartile ranges:
p-value=0.5699
left 90% confidence interval = (5.0,infinity)
</pre>
</td>
</table>

@param other the other bin to compare the receiver against. 
@param resamples the number of times resampling shall be done. 
@param randomGenerator a random number generator. Set this parameter to <tt>null</tt> to use a default random number 
	generator seeded with the current time. 
@param function a difference function comparing two samples; takes as first argument a sample of <tt>this</tt> and as second argument 
	a sample of <tt>other</tt>. 
@return a bootstrap bin holding the results of <tt>function</tt> of each resampling step.
@see cern.colt.GenericPermuting#permutation(long,int)
*/
public synchronized DynamicBin1D sampleBootstrap(DynamicBin1D other, int resamples, cern.jet.random.engine.RandomEngine randomGenerator, BinBinFunction1D function) {
	if (randomGenerator==null) randomGenerator = cern.jet.random.Uniform.makeDefaultGenerator();

	// since "resamples" can be quite large, we care about performance and memory
	int maxCapacity = 1000;
	int s1 = size();
	int s2 = other.size();

	// prepare auxiliary bins and buffers
	DynamicBin1D sample1 = new DynamicBin1D();
	cern.colt.buffer.DoubleBuffer buffer1 = sample1.buffered(Math.min(maxCapacity,s1));
	
	DynamicBin1D sample2 = new DynamicBin1D();
	cern.colt.buffer.DoubleBuffer buffer2 = sample2.buffered(Math.min(maxCapacity,s2));
	
	DynamicBin1D bootstrap = new DynamicBin1D();
	cern.colt.buffer.DoubleBuffer bootBuffer = bootstrap.buffered(Math.min(maxCapacity,resamples));

	// resampling steps
	for (int i=resamples; --i >= 0; ) {
		sample1.clear();
		sample2.clear();
		
		this.sample(s1,true,randomGenerator,buffer1);
		other.sample(s2,true,randomGenerator,buffer2);

		bootBuffer.add(function.apply(sample1,sample2));
	}
	bootBuffer.flush();
	return bootstrap;
}
/**
 * Determines whether the receivers internally preserved elements may be reordered or not.
 * <ul>
 * <li><tt>fixedOrder==false</tt> allows the order in which elements are returned by method <tt>elements()</tt> to be different from the order in which elements are added.
 * <li><tt>fixedOrder==true</tt> guarantees that under all circumstances the order in which elements are returned by method <tt>elements()</tt> is identical to the order in which elements are added.
 * However, the latter consumes twice as much memory if operations involving sorting are requested.
 * This option is usually only required if a 2-dimensional bin, formed by two 1-dimensional bins, needs to be rebinnable.
 * </ul>
 * <p>
 * Naturally, if <tt>fixedOrder</tt> is set to <tt>true</tt> you should not already have added elements to the receiver; it should be empty.
 */
public void setFixedOrder(boolean fixedOrder) {
	//if (size() > 0) throw new RuntimeException("must be called before starting to add elements.");
	this.fixedOrder = fixedOrder;
}
/**
 * Returns the number of elements contained in the receiver.
 *
 * @returns  the number of elements contained in the receiver.
 */
public synchronized int size() {
	return elements.size();
	// Never ever use "this.size" as it would be intuitive!
	// This class abuses "this.size". "this.size" DOES NOT REFLECT the number of elements contained in the receiver!
	// Instead, "this.size" reflects the number of elements incremental stats computation has already processed.
}
/**
 * Sorts elements if not already sorted.
 */
protected void sort() {
	if (!this.isSorted) {
		if (this.fixedOrder) {
			this.sortedElements.clear();
			this.sortedElements.addAllOfFromTo(this.elements, 0, this.elements.size()-1);
			this.sortedElements.sort();
		}
		else {
			/*
			   Call updateIncrementalStats() because after sorting we no more know
			   what elements are still to be done by updateIncrementalStats()
			   and would therefore later need to rebuild incremental stats from scratch.
			*/
			updateIncrementalStats();
			invalidateAll();
					
			this.elements.sort();
			this.isIncrementalStatValid = true;
		}
		this.isSorted = true;
	}
}
/**
 * Returns a copy of the currently stored elements, sorted ascending.
 * Concerning the memory required for operations involving sorting, see {@link #setFixedOrder(boolean)}.
 *
 * @return a copy of the currently stored elements, sorted ascending.
 */
public synchronized DoubleArrayList sortedElements() {
	// safe since we are already synchronized.
	return sortedElements_unsafe().copy();
}
/**
 * Returns the currently stored elements, sorted ascending; <b>WARNING:</b> not a copy of them;
 * Thus, improper usage of the returned list may not only corrupt the receiver's internal state, but also break thread safety!
 * Only provided for performance and memory sensitive applications.
 * Do not modify the returned elements unless you know exactly what you're doing.
 * This method can be used in a thread safe, clean <i>and</i> performant way by
 * explicitly synchronizing on the bin, as follows:
 * <pre>
 * ...
 * synchronized (dynamicBin) { // lock out anybody else
 *     DoubleArrayList elements = dynamicBin.sortedElements_unsafe();
 * 	   // read each element and do something with it, e.g.
 * 	   double[] values = elements.elements(); // zero-copy
 * 	   for (int i=dynamicBin.size(); --i >=0; ) {
 *         foo(values[i]);
 * 	   }			
 * }
 * ...
 * </pre>
 *
 * Concerning the memory required for operations involving sorting, see {@link #setFixedOrder(boolean)}.
 * @return the currently stored elements, sorted ascending.
 */
protected synchronized DoubleArrayList sortedElements_unsafe() {
	sort();
	if (fixedOrder) return this.sortedElements;
	return this.elements;
}
/**
 * Modifies the receiver to be standardized.
 * Changes each element <tt>x[i]</tt> as follows: <tt>x[i] = (x[i]-mean)/standardDeviation</tt>.
 */
public synchronized void standardize(double mean, double standardDeviation) {
	Descriptive.standardize(this.elements, mean, standardDeviation);
	clearAllMeasures();
	invalidateAll();
	this.size = 0;
}
/**
 * Returns the sum of all elements, which is <tt>Sum( x[i] )</tt>.
 */
public synchronized double sum() {
	if (!isIncrementalStatValid) updateIncrementalStats();
	return this.sum;
}
/**
 * Returns the sum of inversions, which is <tt>Sum( 1 / x[i] )</tt>.
 */
public synchronized double sumOfInversions() {
	if (!isSumOfInversionsValid) updateSumOfInversions();
	return this.sumOfInversions;
}
/**
 * Returns the sum of logarithms, which is <tt>Sum( Log(x[i]) )</tt>.
 */
public synchronized double sumOfLogarithms() {
	if (!isSumOfLogarithmsValid) updateSumOfLogarithms();
	return this.sumOfLogarithms;
}
/**
 * Returns the <tt>k-th</tt> order sum of powers, which is <tt>Sum( x[i]<sup>k</sup> )</tt>.
 * @param k the order of the powers.
 * @return the sum of powers.
 */
public synchronized double sumOfPowers(int k) {
	// no chaching for this measure
	if (k >= -1 && k <= 2) return super.sumOfPowers(k);

	return Descriptive.sumOfPowers(this.elements,k);
}
/**
 * Returns the sum of squares, which is <tt>Sum( x[i] * x[i] )</tt>.
 */
public synchronized double sumOfSquares() {
	if (!isIncrementalStatValid) updateIncrementalStats();
	return this.sum_xx;
}
/**
 * Returns a String representation of the receiver.
 */
public synchronized String toString() {
	StringBuffer buf = new StringBuffer(super.toString());
	DoubleArrayList distinctElements = new DoubleArrayList();
	IntArrayList frequencies = new IntArrayList();
	frequencies(distinctElements,frequencies);
	if (distinctElements.size() < 100) { // don't cause unintended floods
		buf.append("Distinct elements: "+distinctElements+"\n");
		buf.append("Frequencies: "+frequencies+"\n");
	}
	else {
		buf.append("Distinct elements & frequencies not printed (too many).");
	}
	return buf.toString();
}
/**
 * Removes the <tt>s</tt> smallest and <tt>l</tt> largest elements from the receiver.
 * The receivers size will be reduced by <tt>s + l</tt> elements.
 *
 * @param s the number of smallest elements to trim away (<tt>s >= 0</tt>).
 * @param l the number of largest elements to trim away (<tt>l >= 0</tt>).
 */
public synchronized void trim(int s, int l) {
	DoubleArrayList elems = sortedElements();
	clear();
	addAllOfFromTo(elems, s, elems.size()-1 - l);
}
/**
 * Returns the trimmed mean.
 * That is the mean of the data <i>if</i> the <tt>s</tt> smallest and <tt>l</tt> largest elements <i>would</i> be removed from the receiver (they are not removed).
 *
 * @param s the number of smallest elements to trim away (<tt>s >= 0</tt>).
 * @param l the number of largest elements to trim away (<tt>l >= 0</tt>).
 * @return the trimmed mean.
 */
public synchronized double trimmedMean(int s, int l) {
	// no caching for this parameter.
	return Descriptive.trimmedMean(sortedElements_unsafe(), mean(), s, l);
}
/**
 * Trims the capacity of the receiver to be the receiver's current size.
 * (This has nothing to do with trimming away smallest and largest elements. The method name is used to be consistent with JDK practice.)
 * <p>
 * Releases any superfluos internal memory.
 * An application can use this operation to minimize the storage of the receiver.
 * Does not affect functionality.
 */
public synchronized void trimToSize() {
	this.elements.trimToSize();
	
	this.sortedElements.clear();
	this.sortedElements.trimToSize();
	if (fixedOrder) this.isSorted = false;
}
/**
 * assertion: isBasicParametersValid == false
 * 
 */
protected void updateIncrementalStats() {
	// prepare arguments
	double[] arguments = new double[4];
	arguments[0] = this.min;
	arguments[1] = this.max;
	arguments[2] = this.sum;
	arguments[3] = this.sum_xx;

	Descriptive.incrementalUpdate(this.elements,this.size, this.elements.size()-1,arguments);

	// store the new parameters back
	this.min =   arguments[0];
	this.max =   arguments[1];
	this.sum =  arguments[2];
	this.sum_xx = arguments[3];

	this.isIncrementalStatValid = true;
	this.size = this.elements.size(); // next time we don't need to redo the stuff we have just done...
}
/**
 * assertion: isBasicParametersValid == false
 * 
 */
protected void updateSumOfInversions() {
	this.sumOfInversions = Descriptive.sumOfInversions(this.elements,0,size()-1);
	this.isSumOfInversionsValid = true;
}
/**
 * 
 */
protected void updateSumOfLogarithms() {
	this.sumOfLogarithms = Descriptive.sumOfLogarithms(this.elements,0,size()-1);
	this.isSumOfLogarithmsValid = true;
}
/**
 * 
 *
 * @param element element to be appended.
 */
protected void validateAll() {
	this.isSorted = true;
	this.isIncrementalStatValid = true;
	
	//this.isSkewValid = true;
	//this.isKurtosisValid = true;
	
	this.isSumOfInversionsValid = true;
	this.isSumOfLogarithmsValid = true;
}
}
