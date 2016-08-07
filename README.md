# GCodeGenerator

Allows to create G-code for roughing for Sinumerik 810 and 840D. Only for lathe.

# Installation

* at the moment not to download from netbeans repository, you can download it from github, open in netbeans and right mouse button on the Project and "Create NBM" to compile it.
The compiled file ".nbm" is in Project folder in the "build" directory.
* Go to "Tools" -> "Plugins" -> "Downloaded", click "Add Plugins..." and select the downloaded file org-roiderh-gcodefunctions.nbm
* Check the Checkbox and click "Install"

# Usage

Select a pice of g-code which describes a contour. Click the Toolbar button.

![Selected g-code which describes the contour](screen_1.png )

Select the generator (for the moment only roughing) and click "Ok"

![Select roughing](screen_2.png )

Edit the parameters for roughing and click "Calculate"

![Create the code](screen_3.png )

The created toolpath. The green line is the contour from the selected g-code, the red line is the generated toolpath for roughing. Click on "G-Code" to show the generated g-code.

![graphic view of the toolpath](screen_4.png)

The g-code for roughing. Select this code and copy it with <kbd>strg</kbd>+<kbd>C</kbd>  and paste it with <kbd>strg</kbd>+<kbd>V</kbd> where ever you want.

![generated g-code](screen_5.png)

