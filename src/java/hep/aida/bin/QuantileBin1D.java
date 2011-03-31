package hep.aida.bin;

import cern.colt.list.DoubleArrayList;
import cern.jet.random.engine.RandomEngine;
import cern.jet.stat.quantile.DoubleQuantileFinder;
import cern.jet.stat.quantile.QuantileFinderFactory;
/**
1-dimensional non-rebinnable bin holding <tt>double</tt> elements with scalable quantile operations defined upon;
Using little main memory, quickly computes approximate quantiles over very large data sequences with and even without a-priori knowledge of the number of elements to be filled;
Conceptually a strongly lossily compressed multiset (or bag);
Guarantees to respect the worst case approximation error specified upon instance construction.
First see the <a href="package-summary.html">package summary</a> and javadoc <a href="package-tree.html">tree view</a> to get the broad picture.
<p>
<b>Motivation and Problem:</b>
Intended to help scale applications requiring quantile computation.
Quantile computation on very large data sequences is problematic, for the following reasons:
Computing quantiles requires sorting the data sequence.
To sort a data sequence the entire data sequence needs to be available.
Thus, data cannot be thrown away during filling (as done by static bins like {@link StaticBin1D} and {@link MightyStaticBin1D}).
It needs to be kept, either in main memory or on disk.
There is often not enough main memory available.
Thus, during filling data needs to be streamed onto disk.
Sorting disk resident data is prohibitively time consuming.
As a consequence, traditional methods either need very large memories (like {@link DynamicBin1D}) or time consuming disk based sorting.
<p>
This class proposes to efficiently solve the problem, at the expense of producing approximate rather than exact results.
It can deal with infinitely many elements without resorting to disk.
The main memory requirements are smaller than for any other known approximate technique by an order of magnitude.
They get even smaller if an upper limit on the maximum number of elements ever to be added is known a-priori.
<p>
<b>Approximation error:</b>
The approximation guarantees are parametrizable and explicit but probabilistic, and apply for arbitrary value distributions and arrival distributions of the data sequence.
In other words, this class guarantees to respect the worst case approximation error specified upon instance construction to a certain probability.
Of course, if it is specified that the approximation error should not exceed some number <i>very close</i> to zero,
this class will potentially consume just as much memory as any of the traditional exact techniques would do.
However, for errors larger than 10<sup>-5</sup>, its memory requirements are modest, as shown by the table below.
<p>
<b>Main memory requirements:</b>
Given in megabytes, assuming a single element (<tt>double</tt>) takes 8 byte.
The number of elements required is then <tt>MB*1024*1024/8</tt>.
<p>
<b>Parameters:</b>
<ul>
<li><i>epsilon</i> - the maximum allowed approximation error on quantiles; in <tt>[0.0,1.0]</tt>.
To get exact rather than approximate quantiles, set <tt>epsilon=0.0</tt>;

<li><i>delta</i> - the probability allowed that the approximation error fails to be smaller than epsilon; in <tt>[0.0,1.0]</tt>.
To avoid probabilistic answers, set <tt>delta=0.0</tt>.
For example, <tt>delta = 0.0001</tt> is equivalent to a confidence of <tt>99.99%</tt>.

<li><i>quantiles</i> - the number of quantiles to be computed; in <tt>[0,Integer.MAX_VALUE]</tt>.

<li><i>is N known?</i> - specifies whether the exact size of the dataset over which quantiles are to be computed is known.

<li>N<sub>max</sub> - the exact dataset size, if known. Otherwise, an upper limit on the dataset size. If no upper limit is known set to infinity (<tt>Long.MAX_VALUE</tt>).
</ul>
	N<sub>max</sub>=inf - we are sure that exactly (<i>known</i>) or less than (<i>unknown</i>) infinity elements will be added.
<br>N<sub>max</sub>=10<sup>6</sup> - we are sure that exactly (<i>known</i>) or less than (<i>unknown</i>) 10<sup>6</sup> elements will be added.
<br>N<sub>max</sub>=10<sup>7</sup> - we are sure that exactly (<i>known</i>) or less than (<i>unknown</i>) 10<sup>7</sup> elements will be added.
<br>N<sub>max</sub>=10<sup>8</sup> - we are sure that exactly (<i>known</i>) or less than (<i>unknown</i>) 10<sup>8</sup> elements will be added.
<p>



<table width="75%" border="1" cellpadding="6" cellspacing="0" align="center">
  <tr align="center" valign="middle"> 
	<td width="20%" nowrap colspan="13" bgcolor="#33CC66"><font color="#000000"></font> 
	  <div align="center"><font color="#000000" size="5">Required main memory 
		[MB]</font></div>
	  </td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="7%" nowrap rowspan="2" bgcolor="#FF9966"><font color="#000000">#quantiles</font></td>
	<td width="6%" nowrap rowspan="2" bgcolor="#FF9966"> 
	  <div align="center"></div>
	  <div align="center"></div>
	  <div align="center"><font color="#000000">epsilon</font></div>
	</td>
	<td width="6%" nowrap rowspan="2" bgcolor="#FF9966"><font color="#000000">delta</font></td>
	<td width="1%" nowrap rowspan="31">&nbsp;</td>
	<td nowrap colspan="4" bgcolor="#FF9966"><font color="#000000">N unknown</font></td>
	<td width="1%" nowrap align="center" valign="middle" bgcolor="#C0C0C0" rowspan="31"><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font></td>
	<td nowrap colspan="4" bgcolor="#FF9966"><font color="#000000">N known</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="7%" nowrap bgcolor="#FF9966"><font color="#000000">N<sub>max</sub>=inf</font></td>
	<td width="9%" nowrap bgcolor="#FF9966"><font color="#000000">N<sub>max</sub>=10<sup>6</sup></font></td>
	<td width="9%" nowrap bgcolor="#FF9966"><font color="#000000">N<sub>max</sub>=10<sup>7</sup></font></td>
	<td width="9%" nowrap bgcolor="#FF9966"><font color="#000000">N<sub>max</sub>=10<sup>8</sup></font></td>
	<td width="8%" nowrap bgcolor="#FF9966"><font color="#000000">N<sub>max</sub>=inf</font></td>
	<td width="9%" nowrap bgcolor="#FF9966"><font color="#000000">N<sub>max</sub>=10<sup>6</sup></font></td>
	<td width="9%" nowrap bgcolor="#FF9966"><font color="#000000">N<sub>max</sub>=10<sup>7</sup></font></td>
	<td width="19%" nowrap bgcolor="#FF9966"><font color="#000000">N<sub>max</sub>=10<sup>8</sup></font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td nowrap bgcolor="#C0C0C0" colspan="3"><font color="#000000"></font> 
	  <div align="center"></div>
	  <font color="#000000"></font></td>
	<td nowrap colspan="4">&nbsp;</td>
	<td nowrap colspan="4">&nbsp;</td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="7%" nowrap bgcolor="#FFCCCC"><font color="#000000">any</font></td>
	<td width="6%" nowrap bgcolor="#FFCCCC"> 
	  <div align="center"><font color="#000000">0</font></div>
	</td>
	<td width="6%" nowrap bgcolor="#FFCCCC"><font color="#000000">any</font></td>
	<td width="7%" nowrap bgcolor="#66CCFF"><font color="#000000">infinity</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">7.6</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">76</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">762</font></td>
	<td width="8%" nowrap bgcolor="#66CCFF"><font color="#000000">infinity</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">7.6</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">76</font></td>
	<td width="19%" nowrap bgcolor="#66CCFF"><font color="#000000">762</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="7%" nowrap rowspan="6" bgcolor="#FFCCCC"><font color="#000000">any</font></td>
	<td width="6%" nowrap bgcolor="#FFCCCC"> 
	  <div align="center"><font color="#000000">10<sup> -1</sup></font></div>
	</td>
	<td width="6%" nowrap rowspan="6" bgcolor="#FFCCCC"><font color="#000000">0</font></td>
	<td width="7%" nowrap rowspan="6" bgcolor="#66CCFF"><font color="#000000">infinity</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.003</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.005</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.006</font></td>
	<td width="8%" nowrap bgcolor="#66CCFF"><font color="#000000">0.03</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.003</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.005</font></td>
	<td width="19%" nowrap bgcolor="#66CCFF"><font color="#000000">0.006</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="6%" nowrap bgcolor="#FFCCCC"> 
	  <div align="center"><font color="#000000">10<sup> -2</sup></font></div>
	</td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.02</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.03</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.05</font></td>
	<td width="8%" nowrap bgcolor="#66CCFF"><font color="#000000">0.31</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.02</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.03</font></td>
	<td width="19%" nowrap bgcolor="#66CCFF"><font color="#000000">0.05</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="6%" nowrap bgcolor="#FFCCCC"> 
	  <div align="center"><font color="#000000">10<sup> -3</sup></font></div>
	</td>
	<td width="9%" nowrap align="center" valign="middle" bgcolor="#66CCFF"><font color="#000000">0.12</font></td>
	<td width="9%" nowrap align="center" valign="middle" bgcolor="#66CCFF"><font color="#000000">0.2</font></td>
	<td width="9%" nowrap align="center" valign="middle" bgcolor="#66CCFF"><font color="#000000">0.3</font></td>
	<td width="8%" nowrap bgcolor="#66CCFF"><font color="#000000">2.7</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.12</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.2</font></td>
	<td width="19%" nowrap bgcolor="#66CCFF"><font color="#000000">0.3</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="6%" nowrap bgcolor="#FFCCCC"> 
	  <div align="center"><font color="#000000">10<sup> -4</sup></font></div>
	</td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.6</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">1.2</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">2.1</font></td>
	<td width="8%" nowrap bgcolor="#66CCFF"><font color="#000000">26.9</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.6</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">1.2</font></td>
	<td width="19%" nowrap bgcolor="#66CCFF"><font color="#000000">2.1</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="6%" nowrap bgcolor="#FFCCCC"> 
	  <div align="center"><font color="#000000">10<sup> -5</sup></font></div>
	</td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">2.5</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">6.4</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">11.6</font></td>
	<td width="8%" nowrap bgcolor="#66CCFF"><font color="#000000">205</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">2.5</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">6.4</font></td>
	<td width="19%" nowrap bgcolor="#66CCFF"><font color="#000000">11.6</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="6%" nowrap bgcolor="#FFCCCC"> 
	  <div align="center"><font color="#000000">10<sup> -6</sup></font></div>
	</td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">7.6</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">25.4</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">63.6</font></td>
	<td width="8%" nowrap bgcolor="#66CCFF"><font color="#000000">1758</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">7.6</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">25.4</font></td>
	<td width="19%" nowrap bgcolor="#66CCFF"><font color="#000000">63.6</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td nowrap bgcolor="#C0C0C0" colspan="3"><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font></td>
	<td nowrap colspan="4">&nbsp;</td>
	<td nowrap colspan="4">&nbsp;</td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="7%" nowrap bgcolor="#FFCCCC" rowspan="8"><font color="#000000">100</font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font></td>
	<td width="6%" nowrap align="center" valign="middle" bgcolor="#FFCCCC" rowspan="2"> 
	  <div align="center"><font color="#000000">10<sup> -2</sup></font></div>
	  <font color="#000000"></font></td>
	<td width="6%" nowrap bgcolor="#FFCCCC"><font color="#000000">10<sup> -1</sup></font></td>
	<td width="7%" nowrap bgcolor="#66CCFF"><font color="#000000">0.033</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.021</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.03</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.03</font></td>
	<td width="8%" nowrap bgcolor="#66CCFF"><font color="#000000">0.020</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.020</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.020</font></td>
	<td width="19%" nowrap bgcolor="#66CCFF"><font color="#000000">0.020</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="6%" nowrap bgcolor="#FFCCCC"><font color="#000000">10<sup> -5</sup></font></td>
	<td width="7%" nowrap bgcolor="#66CCFF"><font color="#000000">0.038</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.021</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.03</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.04</font></td>
	<td width="8%" nowrap bgcolor="#66CCFF"><font color="#000000">0.024</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.020</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.020</font></td>
	<td width="19%" nowrap bgcolor="#66CCFF"><font color="#000000">0.020</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="6%" nowrap align="center" valign="middle" bgcolor="#FFCCCC" rowspan="2"> 
	  <div align="center"><font color="#000000">10<sup> -3</sup></font></div>
	  <font color="#000000"></font></td>
	<td width="6%" nowrap align="center" valign="middle" bgcolor="#FFCCCC"><font color="#000000">10<sup> -1</sup></font></td>
	<td width="7%" nowrap bgcolor="#66CCFF"><font color="#000000">0.48</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.12</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.2</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.3</font></td>
	<td width="8%" nowrap bgcolor="#66CCFF"><font color="#000000">0.32</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.12</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.2</font></td>
	<td width="19%" nowrap bgcolor="#66CCFF"><font color="#000000">0.3</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="6%" nowrap align="center" valign="middle" bgcolor="#FFCCCC"><font color="#000000">10<sup> -5</sup></font></td>
	<td width="7%" nowrap bgcolor="#66CCFF"><font color="#000000">0.54</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.12</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.2</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.3</font></td>
	<td width="8%" nowrap bgcolor="#66CCFF"><font color="#000000">0.37</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.12</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.2</font></td>
	<td width="19%" nowrap bgcolor="#66CCFF"><font color="#000000">0.3</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="6%" nowrap align="center" valign="middle" bgcolor="#FFCCCC" rowspan="2"> 
	  <div align="center"><font color="#000000">10<sup> -4</sup></font></div>
	  <font color="#000000"></font></td>
	<td width="6%" nowrap align="center" valign="middle" bgcolor="#FFCCCC"><font color="#000000">10<sup> -1</sup></font></td>
	<td width="7%" nowrap bgcolor="#66CCFF"><font color="#000000">6.6</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.6</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">1.2</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">2.1</font></td>
	<td width="8%" nowrap bgcolor="#66CCFF"><font color="#000000">4.6</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.6</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">1.2</font></td>
	<td width="19%" nowrap bgcolor="#66CCFF"><font color="#000000">2.1</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="6%" nowrap align="center" valign="middle" bgcolor="#FFCCCC"><font color="#000000">10<sup> -5</sup></font></td>
	<td width="7%" nowrap bgcolor="#66CCFF"><font color="#000000">7.2</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.6</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">1.2</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">2.1</font></td>
	<td width="8%" nowrap bgcolor="#66CCFF"><font color="#000000">5.2</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.6</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">1.2</font></td>
	<td width="19%" nowrap bgcolor="#66CCFF"><font color="#000000">2.1</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="6%" nowrap align="center" valign="middle" bgcolor="#FFCCCC" rowspan="2"> 
	  <div align="center"><font color="#000000">10<sup> -5</sup></font></div>
	  <font color="#000000"></font></td>
	<td width="6%" nowrap align="center" valign="middle" bgcolor="#FFCCCC"><font color="#000000">10<sup> -1</sup></font></td>
	<td width="7%" nowrap bgcolor="#66CCFF"><font color="#000000">86</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">2.5</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">6.4</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">11.6</font></td>
	<td width="8%" nowrap bgcolor="#66CCFF"><font color="#000000">63</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">2.5</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">6.4</font></td>
	<td width="19%" nowrap bgcolor="#66CCFF"><font color="#000000">11.6</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="6%" nowrap align="center" valign="middle" bgcolor="#FFCCCC"><font color="#000000">10<sup> -5</sup></font></td>
	<td width="7%" nowrap bgcolor="#66CCFF"><font color="#000000">94</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">2.5</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">6.4</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">11.6</font></td>
	<td width="8%" nowrap bgcolor="#66CCFF"><font color="#000000">70</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">2.5</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">6.4</font></td>
	<td width="19%" nowrap bgcolor="#66CCFF"><font color="#000000">11.6</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td nowrap bgcolor="#C0C0C0" colspan="3"><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font></td>
	<td nowrap colspan="4">&nbsp;</td>
	<td nowrap colspan="4">&nbsp;</td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="7%" nowrap align="center" valign="middle" bgcolor="#FFCCCC" rowspan="8"><font color="#000000">10000</font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font><font color="#000000"></font></td>
	<td width="6%" nowrap align="center" valign="middle" bgcolor="#FFCCCC" rowspan="2"> 
	  <div align="center"><font color="#000000">10<sup> -2</sup></font></div>
	  <font color="#000000"></font></td>
	<td width="6%" nowrap align="center" valign="middle" bgcolor="#FFCCCC"><font color="#000000">10<sup> -1</sup></font></td>
	<td width="7%" nowrap bgcolor="#66CCFF"><font color="#000000">0.04</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.02</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.03</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.04</font></td>
	<td width="8%" nowrap bgcolor="#66CCFF"><font color="#000000">0.02</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.02</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.02</font></td>
	<td width="19%" nowrap bgcolor="#66CCFF"><font color="#000000">0.02</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="6%" nowrap align="center" valign="middle" bgcolor="#FFCCCC"><font color="#000000">10<sup> -5</sup></font></td>
	<td width="7%" nowrap bgcolor="#66CCFF"><font color="#000000">0.04</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.02</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.03</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.04</font></td>
	<td width="8%" nowrap bgcolor="#66CCFF"><font color="#000000">0.03</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.02</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.03</font></td>
	<td width="19%" nowrap bgcolor="#66CCFF"><font color="#000000">0.03</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="6%" nowrap align="center" valign="middle" bgcolor="#FFCCCC" rowspan="2"> 
	  <div align="center"><font color="#000000">10<sup> -3</sup></font></div>
	  <font color="#000000"></font></td>
	<td width="6%" nowrap align="center" valign="middle" bgcolor="#FFCCCC"><font color="#000000">10<sup> -1</sup></font></td>
	<td width="7%" nowrap bgcolor="#66CCFF"><font color="#000000">0.52</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.12</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.21</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.3</font></td>
	<td width="8%" nowrap bgcolor="#66CCFF"><font color="#000000">0.35</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.12</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.21</font></td>
	<td width="19%" nowrap bgcolor="#66CCFF"><font color="#000000">0.3</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="6%" nowrap align="center" valign="middle" bgcolor="#FFCCCC"><font color="#000000">10<sup> -5</sup></font></td>
	<td width="7%" nowrap bgcolor="#66CCFF"><font color="#000000">0.56</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.12</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.21</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.3</font></td>
	<td width="8%" nowrap bgcolor="#66CCFF"><font color="#000000">0.38</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.12</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.21</font></td>
	<td width="19%" nowrap bgcolor="#66CCFF"><font color="#000000">0.3</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="6%" nowrap align="center" valign="middle" bgcolor="#FFCCCC" rowspan="2"> 
	  <div align="center"><font color="#000000">10<sup> -4</sup></font></div>
	  <font color="#000000"></font></td>
	<td width="6%" nowrap align="center" valign="middle" bgcolor="#FFCCCC"><font color="#000000">10<sup> -1</sup></font></td>
	<td width="7%" nowrap bgcolor="#66CCFF"><font color="#000000">7.0</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.64</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">1.2</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">2.1</font></td>
	<td width="8%" nowrap bgcolor="#66CCFF"><font color="#000000">5.0</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.64</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">1.2</font></td>
	<td width="19%" nowrap bgcolor="#66CCFF"><font color="#000000">2.1</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="6%" nowrap align="center" valign="middle" bgcolor="#FFCCCC"><font color="#000000">10<sup> -5</sup></font></td>
	<td width="7%" nowrap bgcolor="#66CCFF"><font color="#000000">7.5</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.64</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">1.2</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">2.1</font></td>
	<td width="8%" nowrap bgcolor="#66CCFF"><font color="#000000">5.4</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">0.64</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">1.2</font></td>
	<td width="19%" nowrap bgcolor="#66CCFF"><font color="#000000">2.1</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="6%" nowrap align="center" valign="middle" bgcolor="#FFCCCC" rowspan="2"> 
	  <div align="center"><font color="#000000">10<sup> -5</sup></font></div>
	  <font color="#000000"></font></td>
	<td width="6%" nowrap align="center" valign="middle" bgcolor="#FFCCCC"><font color="#000000">10<sup> -1</sup></font></td>
	<td width="7%" nowrap bgcolor="#66CCFF"><font color="#000000">90</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">2.5</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">6.4</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">11.6</font></td>
	<td width="8%" nowrap bgcolor="#66CCFF"><font color="#000000">67</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">2.5</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">6.4</font></td>
	<td width="19%" nowrap bgcolor="#66CCFF"><font color="#000000">11.6</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="6%" nowrap align="center" valign="middle" bgcolor="#FFCCCC"><font color="#000000">10<sup> -5</sup></font></td>
	<td width="7%" nowrap bgcolor="#66CCFF"><font color="#000000">96</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">2.5</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">6.4</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">11.6</font></td>
	<td width="8%" nowrap bgcolor="#66CCFF"><font color="#000000">71</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">2.5</font></td>
	<td width="9%" nowrap bgcolor="#66CCFF"><font color="#000000">6.4</font></td>
	<td width="19%" nowrap bgcolor="#66CCFF"><font color="#000000">11.6</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="19%" nowrap align="center" valign="middle" colspan="3">&nbsp;</td>
	<td width="34%" nowrap colspan="4">&nbsp;</td>
	<td width="45%" nowrap colspan="4">&nbsp;</td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="7%" nowrap align="center" valign="middle" bgcolor="#FF9966" rowspan="2"><font color="#000000">#quantiles</font></td>
	<td width="6%" nowrap align="center" valign="middle" bgcolor="#FF9966" rowspan="2">epsilon</td>
	<td width="6%" nowrap align="center" valign="middle" bgcolor="#FF9966" rowspan="2">delta</td>
	<td width="7%" nowrap bgcolor="#FF9966"><font color="#000000">N<sub>max</sub>=inf</font></td>
	<td width="9%" nowrap bgcolor="#FF9966"><font color="#000000">N<sub>max</sub>=10<sup>6</sup></font></td>
	<td width="9%" nowrap bgcolor="#FF9966"><font color="#000000">N<sub>max</sub>=10<sup>7</sup></font></td>
	<td width="9%" nowrap bgcolor="#FF9966"><font color="#000000">N<sub>max</sub>=10<sup>8</sup></font></td>
	<td width="7%" nowrap bgcolor="#FF9966"><font color="#000000">N<sub>max</sub>=inf</font></td>
	<td width="9%" nowrap bgcolor="#FF9966"><font color="#000000">N<sub>max</sub>=10<sup>6</sup></font></td>
	<td width="9%" nowrap bgcolor="#FF9966"><font color="#000000">N<sub>max</sub>=10<sup>7</sup></font></td>
	<td width="9%" nowrap bgcolor="#FF9966"><font color="#000000">N<sub>max</sub>=10<sup>8</sup></font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td nowrap colspan="4" bgcolor="#FF9966"><font color="#000000">N unknown</font></td>
	<td nowrap colspan="4" bgcolor="#FF9966"><font color="#000000">N known</font></td>
  </tr>
  <tr align="center" valign="middle"> 
	<td width="20%" nowrap align="center" valign="middle" colspan="13" bgcolor="#33CC66"><font color="#000000" size="5">Required 
	  main memory [MB]</font></td>
  </tr>
</table>


<p>
<b>Implementation:</b>
<p>
After: Gurmeet Singh Manku, Sridhar Rajagopalan and Bruce G. Lindsay,
Random Sampling Techniques for Space Efficient Online Computation of Order Statistics of Large Datasets.
Proc. of the 1999 ACM SIGMOD Int. Conf. on Management of Data,
Paper available <A HREF="http://www-cad.eecs.berkeley.edu/~manku/papers/unknown.ps.gz"> here</A>.
<p>
and
<p>
Gurmeet Singh Manku, Sridhar Rajagopalan and Bruce G. Lindsay, 
Approximate Medians and other Quantiles in One Pass and with Limited Memory,
Proc. of the 1998 ACM SIGMOD Int. Conf. on Management of Data,
Paper available <A HREF="http://www-cad.eecs.berkeley.edu/~manku/papers/quantiles.ps.gz"> here</A>.
<p>
The broad picture is as follows. Two concepts are used: <i>Shrinking</i> and <i>Sampling</i>.
Shrinking takes a data sequence, sorts it and produces a shrinked data sequence by picking every k-th element and throwing away all the rest.
The shrinked data sequence is an approximation to the original data sequence.
<p>
Imagine a large data sequence (residing on disk or being generated in memory on the fly) and a main memory <i>block</i> of <tt>n=b*k</tt> elements (<tt>b</tt> is the number of buffers, <tt>k</tt> is the number of elements per buffer). 
Fill elements from the data sequence into the block until it is full or the data sequence is exhausted.
When the block (or a subset of buffers) is full and the data sequence is not exhausted, apply shrinking to lossily compress a number of buffers into one single buffer.
Repeat these steps until all elements of the data sequence have been consumed.
Now the block is a shrinked approximation of the original data sequence. 
Treating it as if it would be the original data sequence, we can determine quantiles in main memory.
<p>
Now, the whole thing boils down to the question of: Can we choose <tt>b</tt> and <tt>k</tt> (the number of buffers and the buffer size) such that <tt>b*k</tt> is minimized, 
yet quantiles determined upon the block are <i>guaranteed</i> to be away from the true quantiles no more than some <tt>epsilon</tt>?
It turns out, we can. It also turns out that the required main memory block size <tt>n=b*k</tt> is usually moderate (see the table above).
<p>
The theme can be combined with random sampling to further reduce main memory requirements, at the expense of probabilistic guarantees.
Sampling filters the data sequence and feeds only selected elements to the algorithm outlined above.
Sampling is turned on or off, depending on the parametrization.
<p>
This quick overview does not go into important details, such as assigning proper <i>weights</i> to buffers, how to choose subsets of buffers to shrink, etc. 
For more information consult the papers cited above.

<p>
<b>Time Performance:</b>
<p>
<div align="center">Pentium Pro 200 Mhz, SunJDK 1.2.2, NT, java -classic,<br>
  filling 10 <sup>4</sup> elements at a time, reading 100 percentiles at a time,<br>
  hasSumOfLogarithms()=false, hasSumOfInversions()=false, getMaxOrderForSumOfPowers()=2<br>
</div>
<center>
  <table border cellpadding="6" cellspacing="0" align="center" width="623">
	<tr valign="middle"> 
	  <td align="center" height="50" colspan="9" bgcolor="#33CC66" nowrap> <font size="5">Performance</font></td>
	</tr>
	<tr valign="middle"> 
	  <td align="center" width="56" height="100" rowspan="2" bgcolor="#FF9966" nowrap> 
		Quantiles</td>
	  <td align="center" width="44" height="100" rowspan="2" bgcolor="#FF9966" nowrap> 
		Epsilon</td>
	  <td align="center" width="32" height="100" rowspan="2" bgcolor="#FF9966" nowrap> 
		Delta</td>
	  <td align="center" width="1" height="150" rowspan="7" nowrap>&nbsp; </td>
	  <td align="center" height="50" colspan="2" bgcolor="#33CC66" nowrap> <font size="5">Filling</font> 
		<br>
		[#elements/sec] </td>
	  <td align="center" width="1" height="150" rowspan="7" nowrap>&nbsp; </td>
	  <td align="center" height="50" colspan="2" bgcolor="#33CC66"> <font size="5">Quantile 
		computation</font><br>
		[#quantiles/sec] </td>
	</tr>
	<tr valign="middle" bgcolor="#FF9966" nowrap> 
	  <td align="center" width="75" height="50" nowrap valign="middle"> <font color="#000000">N 
		unknown,<br>
		N<sub>max</sub>=inf</font></td>
	  <td align="center" width="77" height="50" nowrap valign="middle"> <font color="#000000">N 
		known,<br>
		N<sub>max</sub>=10<sup>7</sup></font> </td>
	  <td align="center" width="106" height="50" nowrap valign="middle"> <font color="#000000">N 
		unknown,<br>
		N<sub>max</sub>=inf</font></td>
	  <td align="center" width="103" height="50" nowrap valign="middle"> <font color="#000000">N 
		known,<br>
		N<sub>max</sub>=10<sup>7</sup></font> </td>
	</tr>
	<tr valign="middle"> 
	  <td align="center" height="31" colspan="3" nowrap>&nbsp; </td>
	  <td align="center" height="31" colspan="2" nowrap>&nbsp; </td>
	  <td align="center" height="31" colspan="2" nowrap>&nbsp; </td>
	</tr>
	<tr valign="middle"> 
	  <td align="center" width="56" rowspan="4" bgcolor="#FFCCCC" nowrap> 10<sup>4</sup></td>
	  <td align="center" width="44" bgcolor="#FFCCCC" nowrap> 10 <sup> -1</sup></td>
	  <td align="center" width="32" bgcolor="#FFCCCC" nowrap rowspan="4"> 10 <sup> 
		-1</sup> </td>
	  <td width="75" bgcolor="#66CCFF" nowrap align="center"> 
		<p>1600000</p>
	  </td>
	  <td width="77" bgcolor="#66CCFF" nowrap align="center">1300000</td>
	  <td align="center" width="106" bgcolor="#66CCFF" nowrap>250000 </td>
	  <td align="center" width="103" bgcolor="#66CCFF" nowrap>130000 </td>
	</tr>
	<tr valign="middle"> 
	  <td align="center" width="44" bgcolor="#FFCCCC"> 10 <sup> -2</sup></td>
	  <td width="75" bgcolor="#66CCFF" align="center">360000</td>
	  <td width="77" bgcolor="#66CCFF" align="center">1200000</td>
	  <td align="center" width="106" bgcolor="#66CCFF">50000 </td>
	  <td align="center" width="103" bgcolor="#66CCFF">20000 </td>
	</tr>
	<tr valign="middle"> 
	  <td align="center" width="44" bgcolor="#FFCCCC"> 10 <sup> -3</sup></td>
	  <td width="75" bgcolor="#66CCFF" align="center">150000</td>
	  <td width="77" bgcolor="#66CCFF" align="center">200000</td>
	  <td align="center" width="106" bgcolor="#66CCFF">3600 </td>
	  <td align="center" width="103" bgcolor="#66CCFF">3000 </td>
	</tr>
	<tr valign="middle"> 
	  <td align="center" width="44" bgcolor="#FFCCCC"> 10 <sup> -4</sup></td>
	  <td width="75" bgcolor="#66CCFF" align="center">120000</td>
	  <td width="77" bgcolor="#66CCFF" align="center">170000</td>
	  <td align="center" width="106" bgcolor="#66CCFF">80 </td>
	  <td align="center" width="103" bgcolor="#66CCFF">1000 </td>
	</tr>
  </table>
</center>

@see cern.jet.stat.quantile
@author wolfgang.hoschek@cern.ch
@version 0.9, 03-Jul-99
*/
public class QuantileBin1D extends MightyStaticBin1D {
	protected DoubleQuantileFinder finder = null;
/**
 * Not public; for use by subclasses only!
 * Constructs and returns an empty bin.
 */
protected QuantileBin1D() {
	super(false,false,2);
}
/**
 * Equivalent to <tt>new QuantileBin1D(false, Long.MAX_VALUE, epsilon, 0.001, 10000, new cern.jet.random.engine.DRand(new java.util.Date())</tt>.
 */
public QuantileBin1D(double epsilon) {
	this(false, Long.MAX_VALUE, epsilon, 0.001, 10000, new cern.jet.random.engine.DRand(new java.util.Date()));
}
/**
 * Equivalent to <tt>new QuantileBin1D(known_N, N, epsilon, delta, quantiles, randomGenerator, false, false, 2)</tt>.
 */
public QuantileBin1D(boolean known_N, long N, double epsilon, double delta, int quantiles, RandomEngine randomGenerator) {
	this(known_N, N, epsilon, delta, quantiles, randomGenerator, false, false, 2);
}
/**
 * Constructs and returns an empty bin that, under the given constraints, minimizes the amount of memory needed.
 *  
 * Some applications exactly know in advance over how many elements quantiles are to be computed.
 * Provided with such information the main memory requirements of this class are small.
 * Other applications don't know in advance over how many elements quantiles are to be computed.
 * However, some of them can give an upper limit, which will reduce main memory requirements.
 * For example, if elements are selected from a database and filled into histograms, it is usually not known in advance how many elements are being filled, but one may know that at most <tt>S</tt> elements, the number of elements in the database, are filled.
 * A third type of application knowns nothing at all about the number of elements to be filled;
 * from zero to infinitely many elements may actually be filled.
 * This method efficiently supports all three types of applications.
 *
 * @param known_N specifies whether the number of elements over which quantiles are to be computed is known or not.
 * <p>
 * @param N if <tt>known_N==true</tt>, the number of elements over which quantiles are to be computed.
 *			if <tt>known_N==false</tt>, the upper limit on the number of elements over which quantiles are to be computed.
 *          In other words, the maximum number of elements ever to be added.
 * 			If such an upper limit is a-priori unknown, then set <tt>N = Long.MAX_VALUE</tt>.
 * <p>
 * @param epsilon the approximation error which is guaranteed not to be exceeded (e.g. <tt>0.001</tt>) (<tt>0 &lt;= epsilon &lt;= 1</tt>). 
 * To get exact rather than approximate quantiles, set <tt>epsilon=0.0</tt>;
 * <p>
 * @param delta the allowed probability that the actual approximation error exceeds <tt>epsilon</tt> (e.g. 0.0001) (0 &lt;= delta &lt;= 1). 
 * To avoid probabilistic answers, set <tt>delta=0.0</tt>.
 * For example, <tt>delta = 0.0001</tt> is equivalent to a confidence of <tt>99.99%</tt>.
 * <p>
 * @param quantiles the number of quantiles to be computed (e.g. <tt>100</tt>) (<tt>quantiles &gt;= 1</tt>). 
 * If unknown in advance, set this number large, e.g. <tt>quantiles &gt;= 10000</tt>.
 * <p>
 * @param generator a uniform random number generator.
 * Set this parameter to <tt>null</tt> to use a default generator seeded with the current time.
 * <p>
 * The next three parameters specify additional capabilities unrelated to quantile computation.
 * They are identical to the one's defined in the constructor of the parent class {@link MightyStaticBin1D}.
 * <p>
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
 */
public QuantileBin1D(boolean known_N, long N, double epsilon, double delta, int quantiles, RandomEngine randomGenerator, boolean hasSumOfLogarithms, boolean hasSumOfInversions, int maxOrderForSumOfPowers) {
	super(hasSumOfLogarithms, hasSumOfInversions, maxOrderForSumOfPowers);
	this.finder = QuantileFinderFactory.newDoubleQuantileFinder(known_N, N, epsilon, delta, quantiles, randomGenerator);
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
	if (this.finder != null) this.finder.addAllOfFromTo(list, from, to);
}
/**
 * Removes all elements from the receiver.
 * The receiver will be empty after this call returns.
 */
public synchronized void clear() {
	super.clear();
	if (this.finder != null) this.finder.clear();
}
/**
 * Returns a deep copy of the receiver.
 *
 * @return a deep copy of the receiver.
 */
public synchronized Object clone() {
	QuantileBin1D clone = (QuantileBin1D) super.clone();
	if (this.finder != null) clone.finder = (DoubleQuantileFinder) clone.finder.clone();
	return clone;
}
/**
 * Computes the deviations from the receiver's measures to another bin's measures.
 * @param other the other bin to compare with
 * @return a summary of the deviations.
 */
public String compareWith(AbstractBin1D other) {
	StringBuffer buf = new StringBuffer(super.compareWith(other));
	if (other instanceof QuantileBin1D) {
		QuantileBin1D q = (QuantileBin1D) other;
		buf.append("25%, 50% and 75% Quantiles: "+relError(quantile(0.25),q.quantile(0.25)) + ", "+ relError(quantile(0.5),q.quantile(0.5)) + ", " + relError(quantile(0.75),q.quantile(0.75)));
		buf.append("\nquantileInverse(mean): "+relError(quantileInverse(mean()),q.quantileInverse(q.mean())) +" %");
		buf.append("\n");
	}
	return buf.toString();
}
/**
 * Returns the median.
 */
public double median() {
	return quantile(0.5);
}
/**
 * Computes and returns the phi-quantile.
 * @param phi the percentage for which the quantile is to be computed.
 * phi must be in the interval <tt>(0.0,1.0]</tt>.
 * @return the phi quantile element.
 */
public synchronized double quantile(double phi) {
	return quantiles(new DoubleArrayList(new double[] {phi})).get(0);
}
/**
 * Returns how many percent of the elements contained in the receiver are <tt>&lt;= element</tt>.
 * Does linear interpolation if the element is not contained but lies in between two contained elements.
 *
 * @param the element to search for.
 * @return the percentage <tt>phi</tt> of elements <tt>&lt;= element</tt> (<tt>0.0 &lt;= phi &lt;=1.0)</tt>.
 */
public synchronized double quantileInverse(double element) {
	return finder.phi(element);
}
/**
 * Returns the quantiles of the specified percentages.
 * For implementation reasons considerably more efficient than calling {@link #quantile(double)} various times.
 * @param percentages the percentages for which quantiles are to be computed.
 * Each percentage must be in the interval <tt>(0.0,1.0]</tt>. <tt>percentages</tt> must be sorted ascending.
 * @return the quantiles.
 */
public synchronized DoubleArrayList quantiles(cern.colt.list.DoubleArrayList phis) {
	return finder.quantileElements(phis); 
}
/**
 * Returns how many elements are contained in the range <tt>[minElement,maxElement]</tt>.
 * Does linear interpolation if one or both of the parameter elements are not contained.
 * Returns exact or approximate results, depending on the parametrization of this class or subclasses.
 *
 * @param minElement the minimum element to search for.
 * @param maxElement the maximum element to search for.
 * @return the number of elements in the range.
 */
public int sizeOfRange(double minElement, double maxElement) {
	return (int) Math.round(size() * (quantileInverse(maxElement) - quantileInverse(minElement)));
}
/**
Divides (rebins) a copy of the receiver at the given <i>percentage boundaries</i> into bins and returns these bins, such that each bin <i>approximately</i> reflects the data elements of its range.
 
The receiver is not physically rebinned (divided); it stays unaffected by this operation.
The returned bins are such that <i>if</i> one would have filled elements into multiple bins
instead of one single all encompassing bin only, those multiple bins would have <i>approximately</i> the same statistics measures as the one's returned by this method.
<p>
The <tt>split(...)</tt> methods are particularly well suited for real-time interactive rebinning (the famous "scrolling slider" effect).
<p>
Passing equi-distant percentages like <tt>(0.0, 0.2, 0.4, 0.6, 0.8, 1.0)</tt> into this method will yield bins of an <i>equi-depth histogram</i>, i.e. a histogram with bin boundaries adjusted such that each bin contains the same number of elements, in this case 20% each.
Equi-depth histograms can be useful if, for example, not enough properties of the data to be captured are known a-priori to be able to define reasonable bin boundaries (partitions).
For example, when guesses about minimas and maximas are strongly unreliable.
Or when chances are that by focussing too much on one particular area other important areas and characters of a data set may be missed.
<p>
<b>Implementation:</b>
<p>
The receiver is divided into <tt>s = percentages.size()-1</tt> intervals (bins).
For each interval <tt>I</tt>, its minimum and maximum elements are determined based upon quantile computation.
Further, each interval <tt>I</tt> is split into <tt>k</tt> equi-percent-distant subintervals (sub-bins).
In other words, an interval is split into subintervals such that each subinterval contains the same number of elements.
<p>
For each subinterval <tt>S</tt>, its minimum and maximum are determined, again, based upon quantile computation.
They yield an approximate arithmetic mean <tt>am = (min+max)/2</tt> of the subinterval.
A subinterval is treated as if it would contain only elements equal to the mean <tt>am</tt>.
Thus, if the subinterval contains, say, <tt>n</tt> elements, it is assumed to consist of <tt>n</tt> mean elements <tt>(am,am,...,am)</tt>.
A subinterval's sum of elements, sum of squared elements, sum of inversions, etc. are then approximated using such a sequence of mean elements.
<p>
Finally, the statistics measures of an interval <tt>I</tt> are computed by summing up (integrating) the measures of its subintervals.
<p>
<b>Accuracy</b>:
<p>
Depending on the accuracy of quantile computation and the number of subintervals per interval (the resolution).
Objects of this class compute exact or approximate quantiles, depending on the parameters used upon instance construction.
Objects of subclasses may <i>always</i> compute exact quantiles, as is the case for {@link DynamicBin1D}.
Most importantly for this class <tt>QuantileBin1D</tt>, a reasonably small epsilon (e.g. 0.01, perhaps 0.001) should be used upon instance construction.
The confidence parameter <tt>delta</tt> is less important, you may find <tt>delta=0.00001</tt> appropriate.
<br>
The larger the resolution, the smaller the approximation error, up to some limit.
Integrating over only a few subintervals per interval will yield very crude approximations.
If the resolution is set to a reasonably large number, say 10..100, more small subintervals are integrated, resulting in more accurate results.
<br>
Note that for good accuracy, the number of quantiles computable with the given approximation guarantees should upon instance construction be specified, so as to satisfy
<p>
<tt>quantiles > resolution * (percentages.size()-1)</tt>
<p>
<p>
<b>Example:</b>
<p>
<tt>resolution=2, percentList = (0.0, 0.1, 0.2, 0.5, 0.9, 1.0)</tt> means the receiver is to be split into 5 bins:
<br>
<ul>
<li>bin 0 ranges from <tt>[0%..10%)</tt> and holds the smallest 10% of the sorted elements.
<li>bin 1 ranges from <tt>[10%..20%)</tt> and holds the next smallest 10% of the sorted elements.
<li>bin 2 ranges from <tt>[20%..50%)</tt> and holds the next smallest 30% of the sorted elements.
<li>bin 3 ranges from <tt>[50%..90%)</tt> and holds the next smallest 40% of the sorted elements.
<li>bin 4 ranges from <tt>[90%..100%)</tt> and holds the largest 10% of the sorted elements.
</ul>
<p>
The statistics measures for each bin are to be computed at a resolution of 2 subbins per bin.
Thus, the statistics measures of a bin are the integrated measures over 2 subbins, each containing the same amount of elements:
<ul>
<li>bin 0 has a subbin ranging from <tt>[ 0%.. 5%)</tt> and a subbin ranging from <tt>[ 5%..10%)</tt>.
<li>bin 1 has a subbin ranging from <tt>[10%..15%)</tt> and a subbin ranging from <tt>[15%..20%)</tt>.
<li>bin 2 has a subbin ranging from <tt>[20%..35%)</tt> and a subbin ranging from <tt>[35%..50%)</tt>.
<li>bin 3 has a subbin ranging from <tt>[50%..70%)</tt> and a subbin ranging from <tt>[70%..90%)</tt>.
<li>bin 4 has a subbin ranging from <tt>[90%..95%)</tt> and a subbin ranging from <tt>[95%..100%)</tt>.
</ul>
<p>
Lets concentrate on the subbins of bin 0.
<ul>
<li>Assume the subbin <tt>A=[0%..5%)</tt> has a minimum of <tt>300</tt> and a maximum of <tt>350</tt> (0% of all elements are less than 300, 5% of all elements are less than 350).
<li>Assume the subbin <tt>B=[5%..10%)</tt> has a minimum of <tt>350</tt> and a maximum of <tt>550</tt> (5% of all elements are less than 350, 10% of all elements are less than 550).
</ul>
<p>
Assume the entire data set consists of <tt>N=100</tt> elements.
<ul>
<li>Then subbin A has an approximate mean of <tt>300+350 / 2 = 325</tt>, 
a size of <tt>N*(5%-0%) = 100*5% = 5</tt> elements, an approximate sum of <tt>325 * 100*5% = 1625</tt>, an approximate sum of squares of <tt>325<sup>2</sup> * 100*5% = 528125</tt>, an approximate sum of inversions of <tt>(1.0/325) * 100*5% = 0.015</tt>, etc.
<li>Analogously, subbin B has an approximate mean of <tt>350+550 / 2 = 450</tt>,
a size of <tt>N*(10%-5%) = 100*5% = 5</tt> elements, an approximate sum of <tt>450 * 100*5% = 2250</tt>, an approximate sum of squares of <tt>450<sup>2</sup> * 100*5% = 1012500</tt>, an approximate sum of inversions of <tt>(1.0/450) * 100*5% = 0.01</tt>, etc.
</ul>
<p>
Finally, the statistics measures of bin 0 are computed by summing up (integrating) the measures of its subintervals:
Bin 0 has a size of <tt>N*(10%-0%)=10</tt> elements (we knew that already), sum of <tt>1625+2250=3875</tt>, sum of squares of <tt>528125+1012500=1540625</tt>, sum of inversions of <tt>0.015+0.01=0.025</tt>, etc.
From these follow other measures such as <tt>mean=3875/10=387.5, rms = sqrt(1540625 / 10)=392.5</tt>, etc.
The other bins are computes analogously.
 
@param percentages the percentage boundaries at which the receiver shall be split.
@param resolution a measure of accuracy; the desired number of subintervals per interval. 
*/
public synchronized MightyStaticBin1D[] splitApproximately(DoubleArrayList percentages, int k) {
	/*
	   percentages = [p0, p1, p2, ..., p(size-2), p(size-1)]
	   defines bins [p0,p1), [p1,p2), ..., [p(size-2),p(size-1))
	   each bin is divided into k equi-percent-distant sub bins (subintervals).
	   e.g. k = 2 means "compute" with a resolution (accuracy) of 2 subbins (subintervals) per bin,
	   
	   percentages = [0.1, 0.2, 0.3, ..., 0.9, 1.0] means
	   bin 0 holds the first 0.1-0.0=10% of the sorted elements,
	   bin 1 holds the next  0.2-0.1=10% of the sorted elements,
	   ...
	   
	   bins =          [0.1, 0.2), [0.2, 0.3), ..., [0.9, 1.0)
	         subBins = [0.1,    0.15,     0.2,     0.25,    0.3,    ....]
	                                                                  
	                   [0.1, 0.15) [0.15, 0.2)             [0.3, 0.35) [0.35, 0.4)
	                                        
 	                                     [0.2, 0.25) [0.25, 0.3)
 	   
 	 */
	int percentSize = percentages.size();
	if (k<1 || percentSize < 2) throw new IllegalArgumentException();
	
	double[] percent = percentages.elements();
	int noOfBins = percentSize-1;


	// construct subintervals
	double[] subBins = new double[1 + k*(percentSize-1)];	
	subBins[0] = percent[0];
	int c = 1;

	for (int i=0; i < noOfBins; i++) {
		double step = (percent[i+1] - percent[i]) / k;
		for (int j=1; j <= k; j++) {
			subBins[c++] = percent[i] + j*step;
		}
	}

	// compute quantile elements;
	double[] quantiles = quantiles(new DoubleArrayList(subBins)).elements();

	// collect summary statistics for each bin.
	// one bin's statistics are the integrated statistics of its subintervals.
	MightyStaticBin1D[] splitBins = new MightyStaticBin1D[noOfBins];
	int maxOrderForSumOfPowers =  getMaxOrderForSumOfPowers();
	maxOrderForSumOfPowers = Math.min(10,maxOrderForSumOfPowers); // don't compute tons of measures
	
	int dataSize = this.size();
	c = 0;
	for (int i=0; i < noOfBins; i++) { // for each bin
		double step = (percent[i+1] - percent[i]) / k;
		double binSum = 0;
		double binSumOfSquares = 0;
		double binSumOfLogarithms = 0;
		double binSumOfInversions = 0;
		double[] binSumOfPowers = null;
		if (maxOrderForSumOfPowers>2) {
			binSumOfPowers = new double[maxOrderForSumOfPowers-2];
		}
		
		double binMin = quantiles[c++];
		double safe_min = binMin;
		double subIntervalSize = dataSize*step;

		for (int j=1; j <= k; j++) { // integrate all subintervals
			double binMax = quantiles[c++];
			double binMean = (binMin+binMax)/2;
			binSum += binMean * subIntervalSize;
			binSumOfSquares += binMean*binMean * subIntervalSize;
			if (this.hasSumOfLogarithms) {
				binSumOfLogarithms += (Math.log(binMean)) * subIntervalSize;
			}
			if (this.hasSumOfInversions) {
				binSumOfInversions += (1/binMean) * subIntervalSize;
			}
			if (maxOrderForSumOfPowers>=3) binSumOfPowers[0] += binMean * binMean * binMean * subIntervalSize;
			if (maxOrderForSumOfPowers>=4) binSumOfPowers[1] += binMean * binMean * binMean * binMean * subIntervalSize;
			for (int p=5; p<=maxOrderForSumOfPowers; p++) {
				binSumOfPowers[p-3] += Math.pow(binMean,p) * subIntervalSize;
			}

			binMin = binMax;
		}
		c--;

		// example: bin(0) contains (0.2-0.1) == 10% of all elements
		int binSize = (int) Math.round((percent[i+1] - percent[i]) * dataSize);
		double binMax = binMin;
		binMin = safe_min;

		// fill statistics
		splitBins[i] = new MightyStaticBin1D(this.hasSumOfLogarithms, this.hasSumOfInversions, maxOrderForSumOfPowers);
		if (binSize>0) {
			splitBins[i].size = binSize;
			splitBins[i].min = binMin;
			splitBins[i].max = binMax;
			splitBins[i].sum = binSum;
			splitBins[i].sum_xx = binSumOfSquares;
			splitBins[i].sumOfLogarithms = binSumOfLogarithms;
			splitBins[i].sumOfInversions = binSumOfInversions;
			splitBins[i].sumOfPowers = binSumOfPowers;
		}
		/*
		double binMean = binSum / binSize;
		System.out.println("size="+binSize);
		System.out.println("min="+binMin);
		System.out.println("max="+binMax);
		System.out.println("mean="+binMean);
		System.out.println("sum_x="+binSum);
		System.out.println("sum_xx="+binSumOfSquares);
		System.out.println("rms="+Math.sqrt(binSumOfSquares / binSize));
		System.out.println();
		*/	
		
	}
	return splitBins;
}
/**
Divides (rebins) a copy of the receiver at the given <i>interval boundaries</i> into bins and returns these bins, such that each bin <i>approximately</i> reflects the data elements of its range.

For each interval boundary of the axis (including -infinity and +infinity), computes the percentage (quantile inverse) of elements less than the boundary.
Then lets {@link #splitApproximately(DoubleArrayList,int)} do the real work.

@param axis an axis defining interval boundaries.
@param resolution a measure of accuracy; the desired number of subintervals per interval. 
*/
public synchronized MightyStaticBin1D[] splitApproximately(hep.aida.IAxis axis, int k) {
	DoubleArrayList percentages = new DoubleArrayList(new hep.aida.ref.Converter().edges(axis));
	percentages.beforeInsert(0,Double.NEGATIVE_INFINITY);
	percentages.add(Double.POSITIVE_INFINITY);
	for (int i=percentages.size(); --i >= 0; ) {
		percentages.set(i, quantileInverse(percentages.get(i)));
	}
	
	return splitApproximately(percentages,k); 
}
/**
 * Returns a String representation of the receiver.
 */
public synchronized String toString() {
	StringBuffer buf = new StringBuffer(super.toString());
	buf.append("25%, 50%, 75% Quantiles: "+quantile(0.25) + ", "+ quantile(0.5) + ", " + quantile(0.75));
	//buf.append("10%, 25%, 50%, 75%, 90% Quantiles: "+quantile(0.1) + ", "+ quantile(0.25) + ", "+ quantile(0.5) + ", " + quantile(0.75) + ", " + quantile(0.9));
	buf.append("\nquantileInverse(median): "+quantileInverse(median()));
	buf.append("\n");
	return buf.toString();
}
}
