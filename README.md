# zio-film-service
zio-film-service

# Exception Handling
```agsl
sealed trait AppError {
  def getMessage: String
}

final case class CustomException(msg: String, cause: Option[Throwable] = None) extends Throwable(msg) with AppError {
  cause.foreach(initCause)
}
```
This part of the code defines a custom exception CustomException that can be used throughout the application to handle specific error cases. The CustomException is a final case class extending Throwable, and it also mixes in a trait AppError, signaling that it's an application-level error.

# Running Movie Service
```agsl
class RunMovieService extends MovieService {
  def getParentalControlLevel(movieId: String): IO[CustomException, String] = ...
}
```
RunMovieService is an implementation of the MovieService trait. It provides the implementation for the getParentalControlLevel method that takes a movieId and returns the parental control level of the corresponding movie. If the movie is not found, a custom exception is thrown.

# Starting the Service
```agsl
object StartService extends App {
  private type AppEnvironment = Has[CmdClient] with Console
  // Layers, app, and main method
}
```
StartService is the entry point of the application. It defines the main ZIO application that sets up layers for dependencies and runs the main logic. The application's dependencies include a movie service, console service, and command-line client (CmdClient).

# Configuration Loader
```
object ConfigLoader {
  def loadConfigFromFile: Task[String] = ...
  def parseConfig(config: String): Task[List[String]] = ...
}
```
ConfigLoader is responsible for reading and parsing a JSON configuration file that defines various settings such as movie ratings. It has two main methods, one for loading the file and the other for parsing the contents into a list of strings.

# Services
``` 
object Services {
trait ParentalControlService { ... }
trait CmdClient { ... }
object ParentalControlService { ... }
object CmdClient { ... }
} 
``` 

This package defines the main services of the application:
ParentalControlService: An interface for determining if a user is allowed to watch a specific movie based on their preferred parental control level.
CmdClient: An interface for interacting with the user via the command line,allowing them to input their preferences and movie choice.
Implementations of both these services are provided using ZIO layers, and they depend on other components such as MovieService and Console.

# Third-Party Movie Interface
```
trait MovieService {
  def getParentalControlLevel(movieId: String): IO[CustomException, String]
}
```

This trait defines an interface for interacting with a movie service. The main method, getParentalControlLevel, takes a movie ID and returns the corresponding parental control level, or an error if the movie is not found.

# Summary
The provided code represents a complete system for a Parental Control Service. The user interacts with the system through the command line to choose their preferred parental control level and select a movie. The system checks if the user is allowed to watch the chosen movie based on the parental control level, using a movie service to fetch the movie's rating. Configuration is loaded from a file, and custom error handling is used to handle exceptional cases.

The code leverages the ZIO library for handling effects and dependencies, providing a structure that encourages separation of concerns and testability. The use of custom exceptions, traits, and layers facilitates a clean and modular design.

    
