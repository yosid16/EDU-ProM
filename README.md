# EDU-ProM

An library for working with ProM capabilities in a non-interactive approach. Enables running common mining task from the ProM framework as well as extending them. Also supports additional capabilitied based on ProM such as - image exporting, conversions etc.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

```
1.	Install JDK (version > 1.8): https://www3.ntu.edu.sg/home/ehchua/programming/howto/JDK_Howto.html
2.	Install Intellij-IDEA: https://www.jetbrains.com/idea/
3.	Install Git: https://git-scm.com/downloads
4.	Install Gradle (version <= 3.3, careful NOT to install 3.4): https://gradle.org/install
```
### Installing


Configurations: 
```
1.	Open the repository in IntelliJ-IDEA, by choosing the gradle file. When asked, select to open as a project. When asked, choose. auto-import for gradle, and specify the gradle home path. Wait several moments, as long as the dependency import is in progress.
2.	Make sure that the general compiler settings are set to java >=1.8, and that the project settings are also set correctly
3.	Try to build the project (Ctrl+F9), if there are any compilation errors, try to open terminal and run: gradle clean build
4.	After the build is successful, the main program should be set. That can be done via running it once manually:
``` 
Sanity checks:
```
1.	The program should produce debugging log at \Logs\bpm.log
2.	Alignment and conformance measurements should be printed to console.
3.	The exit code should be 0.
4.	An image, describing the process model should be at \Output\sample.png

```
## Built With

* [GRADLE](https://gradle.org) 


## Authors

* **Billie Thompson** - *Initial work* - [PurpleBooth](https://github.com/PurpleBooth)

See also the list of [contributors](https://github.com/your/project/contributors) who participated in this project.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

* RapidProM & ProM framework - https://github.com/rapidprom/rapidprom-source
