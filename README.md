# FormulaSystem
Set up a system of mathmatical equations and variables. Then you supply variables one by on, as you do this, all variables that are possible with the ones you have supplied get automatically calculated.

# How to use
In the file 'FormulaSystem', there are two methodical types of class: The 'FormulaSystem' and two 'Formula' classes.  
First, you need to set up some kind of list that contains all formulast hat describe your system. 
I recommend using the 'SimpleFormula' class, for more complex use cases, you can also extend the 'Formula' class
Every 'SimpleFormula'-instance has some fixed variables, supplied to the constructor and, supplied by calling the 'addSolve'-method, 
you can supply solve-functions for every variable that solves for the associated variable respectively.

After you created a FormulaSystem, supplying your list of formulas, you can start adding known variables with the 'set()' and 'add()' methods. 
You can access all variables through the 'knownVariables' property of FormulaSystem. These resemble all the variables that can be calculated with 
the supplied variables and formulas.
