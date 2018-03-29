This zip file contains the CDA Content Evaluation "Gold standard" documents
and the asserts used to evaluate content for those documents.  Only documents
which you had selected in the web application list at the time you clicked on
the "Download zip file of selected document(s)" command button are included.

In addition to this Readme.txt file, there are two files in the zip for each
selected document type. (Both file names begin with the document type itself.)
They are:

     document_type_name.gold-document.xml - The "gold standard" document
     document_type_name.assertions.txt   - The assertions for the document type.
     
The "gold standard" document is simply a sample CDA document of its document 
type which, at our current stage of enlightenment, we believe is correct. This 
is the document which is compared to your test documents when you run CDA 
Content Evaluation.

The assertions are a series of statements about the document type which CDA
content evaluation uses to check your test documents. Assertions are tab 
delimited lines with three fields:

     Command Code - One of EQ, SIMILAR, and PRESENT.
     A Node name - in XPath format representing an element or attribute in the
                   gold standard document.
     A comment - Describing the assertion

For EQ commands, the node must be present in the test document and its value 
must exactly equal the value in the gold standard document.

For SIMILAR commands, the node must be present and have a value like the value
in the gold standard document, but not exactly the same.

For PRESENT commands, the node must exist in the test document, but its value is
not considered.

Regards,
Ralph Moulton 
moultonr@mir.wustl.edu