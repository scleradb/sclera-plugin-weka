# Sclera - Weka Connector

Enables Sclera to perform [classification](http://en.wikipedia.org/wiki/Cluster_analysis) and [clustering](http://en.wikipedia.org/wiki/Cluster_analysis) on data from within SQL.

With this component, a classifier or a clusterer can be trained in just a single SQL command. Scoring new data using the classifier, or segmenting data using the clusterer gets done using a simple SQL operator (Sclera's extension) that seamlessly embeds within your SQL query.

The component uses the [Weka](http://www.cs.waikato.ac.nz/ml/weka) library, which is downloaded automatically as a part of the installation.

Please refer to the [ScleraSQL Reference](/doc/ref/sqlextml#sclera-weka) document for details on using the component's features in a SQL query.

**Important**
The [Weka](http://www.cs.waikato.ac.nz/ml/weka) library is licensed under the [GNU General Public License version 2](http://www.gnu.org/licenses/old-licenses/gpl-2.0.html). For [compatibility](http://www.gnu.org/licenses/gpl-faq.html#AllCompatibility), this component is licensed under the [GNU General Public License version 2](http://www.gnu.org/licenses/old-licenses/gpl-2.0.html) as well. Please use this component in accordance with this license. To get a commercial license for Weka, please refer to the [Weka FAQ](http://weka.wikispaces.com/Can+I+use+WEKA+in+commercial+applications%3F).

*This component is an OPTIONAL extension. As such, this component's license does NOT affect your use of any other Sclera component, or the core Sclera platform.*
