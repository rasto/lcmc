package hep.aida.bin;

import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;
/**
 * 1-dimensional non-rebinnable bin consuming <tt>double</tt> elements;
 * Efficiently computes basic statistics of data sequences.
 * First see the <a href="package-summary.html">package summary</a> and javadoc <a href="package-tree.html">tree view</a> to get the broad picture.
 * <p>
 * The data streamed into a <tt>SimpleBin1D</tt> is not preserved!
 * As a consequence infinitely many elements can be added to this bin.
 * As a further consequence this bin cannot compute more than basic statistics.
 * It is also not rebinnable.
 * If these drawbacks matter, consider to use a {@link DynamicBin1D}, 
 * which overcomes them at the expense of increased memory requirements.
 * <p>
 * This class is fully thread safe (all public methods are synchronized).
 * Thus, you can have one or more threads adding to the bin as well as one or more threads reading and viewing the statistics of the bin <i>while it is filled</i>.
 * For high performance, add data in large chunks (buffers) via method <tt>addAllOf</tt> rather than piecewise via method <tt>add</tt>.
 * <p>
 * <b>Implementation</b>:
 * Incremental maintainance. Performance linear in the number of elements added.
 * 
 * @author wolfgang.hoschek@cern.ch
 * @version 0.9, 03-Jul-99
 */
public class StaticBin1D extends AbstractBin1D {
	/**
	 * The number of elements consumed by incremental parameter maintainance.
	 */
	protected int size = 0;

	// cached parameters
	protected double min = 0.0;    // Min( x[i] )
	protected double max = 0.0;    // Max( x[i] )
	protected double sum = 0.0;    // Sum( x[i] )
	protected double sum_xx = 0.0; // Sum( x[i]*x[i] )

	/** 
	 * Function arguments used by method addAllOf(...)
	 * For memory tuning only. Avoids allocating a new array of arguments each time addAllOf(...) is called.
	 *
	 * Each bin does not need its own set of argument vars since they are declared as "static".
	 * addAllOf(...) of this class uses only 4 entries.
	 * Subclasses computing additional incremental statistics may need more arguments.
	 * So, to be on the safe side we allocate space for 20 args.
	 * Be sure you access this arguments only in synchronized blocks like
	 * synchronized (arguments) { do it }
	 *
	 * By the way, the whole fuss would be unnecessary if Java would know INOUT parameters (call by reference).
	 */
	static transient protected double[] arguments = new double[20];
/**
 * Constructs and returns an empty bin.
 */
public StaticBin1D() {
	clear();
}
/**
 * Adds the specified element to the receiver.
 *
 * @param element element to be appended.
 */
public synchronized void add(double element) {
	// prototyping implementation; inefficient; TODO
	this.addAllOf(new DoubleArrayList(new double[] {element}));
	/*
	sumSquares += element * element;
	if (this.done == 0) { // initial setup
		this.min = element;
		this.max = element;
	}
	else {
		if (element < this.min) this.min = element;
		if (element > this.max) this.max = element;

		double oldMean = this.mean;
		this.mean += (element - this.mean)/(done+1);
		this.sumsq += (element-this.mean)*(element-oldMean); // cool, huh?
	}
	this.done++;
	*/
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
	//if (this.arguments == null) setUpCache();
	synchronized (arguments) {
		// prepare arguments
		arguments[0] = this.min;
		arguments[1] = this.max;
		arguments[2] = this.sum;
		arguments[3] = this.sum_xx;

		Descriptive.incrementalUpdate(list, from, to, arguments);

		// store the new parameters back
		this.min = arguments[0];
		this.max = arguments[1];
		this.sum = arguments[2];
		this.sum_xx = arguments[3];

		this.size += to-from+1;
	}
}
/**
 * Removes all elements from the receiver.
 * The receiver will be empty after this call returns.
 */
public synchronized void clear() {
	clearAllMeasures();
	this.size = 0;
}
/**
 * Resets the values of all measures.
 */
protected void clearAllMeasures() {
	this.min = Double.POSITIVE_INFINITY;
	this.max = Double.NEGATIVE_INFINITY;
	this.sum = 0.0;
	this.sum_xx = 0.0;
}
/**
 * Returns <tt>false</tt>.
 * Returns whether a client can obtain all elements added to the receiver.
 * In other words, tells whether the receiver internally preserves all added elements.
 * If the receiver is rebinnable, the elements can be obtained via <tt>elements()</tt> methods.
 *
 */
public synchronized boolean isRebinnable() {
	return false;
}
/**
 * Returns the maximum.
 */
public synchronized double max() {
	return this.max;
}
/**
 * Returns the minimum.
 */
public synchronized double min() {
	return this.min;
}
/**
 * Returns the number of elements contained in the receiver.
 *
 * @returns  the number of elements contained in the receiver.
 */
public synchronized int size() {
	return this.size;
}
/**
 * Returns the sum of all elements, which is <tt>Sum( x[i] )</tt>.
 */
public synchronized double sum() {
	return this.sum;
}
/**
 * Returns the sum of squares, which is <tt>Sum( x[i] * x[i] )</tt>.
 */
public synchronized double sumOfSquares() {
	return this.sum_xx;
}
}
