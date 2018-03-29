This readme file contains instructions on the format of assertion files in
the Documents and Assertions subdirectories.  See CDAContentEvaluation.ini for
additional information.

Assertion file:

ONE and ONLY ONE assertion file MUST exist in each document subdirectory of the
Documents directory. This is the assertion file for that document. These files 
may refer to any number of assertion files in the Assertions directory via the
INSERT command. Assertion files in the Assertions directory MAY NOT refer to 
other assertion files using the INSERT command; They may not contain any 
INSERT commands.

1 Names must end in ".txt", the rest of the name may be any valid file name.
2 Must be readable.
3 May contain any number of comment lines. Comment lines include blank lines,
  and lines whose first non-whitespace character is ";", "#", or "@".
4 Comment lines beginning with ";" are considered to be 'internal' comments; 
  they will not be visible to users. All other comments will appear in the
  assertions window in the web app.
  "@" comments will also appear on the evaluation listing.
5 Leading and trailing whitespace will be trimmed from all assertion file lines
  on load (but not from the actual file on disc).
6 Command lines (non-comments) are tab delimited, of the form:
  Command-code \t Xpath desc \t comment.
7 Valid command codes are: EQ, SIMILAR, PRESENT, and INSERT. They are not case
  sensitive.
8 Xpath expressions are processed in non Namespace Aware mode; Nodes should not
  include namespace prefixes.
9 Insert commands are of the form: 
  INSERT \t file-name
  where file name is the name of another assertion file whose assertions are to
  be inserted into this assertion file in place of the INSERT command. The
  file-name is a path relative to the Assertions directory. If file-name does
  not end in ".txt", ".txt" will be added. Only assertion files which are in
  the Documents directory tree can have INSERT commands. External comments in
  an insert assertions file will be copied into files which reference it, along
  with EQ, SIMILAR, and PRESENT commands, so they will also be visible to the 
  user.
