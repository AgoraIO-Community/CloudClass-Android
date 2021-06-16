_English | [中文](README.zh.md)_

This page introduces how to run the Android sample project.

## How to run the sample project

### Prerequisites 

- Make sure you have made the preparations mentioned in the [Agora e-Education Guide](https://github.com/AgoraIO-Usecase/eEducation).
- Prepare the development environment:
  - JDK
  - Android Studio 2.0  or later
- Real Android devices, such as Nexus 5X. We recommend using real devices because some function may not work well on simulators or you may encounter performance issues.

### Run the sample project

- Obtain the App ID, App Certificate in the agora.io console and configure it in string_config.xml.
- Two host-related configurations in string_config.xml are ignored.
- Sync and run the project.
> *However, we strongly do not recommend this insecure solution for locally generating rtmToken. For specific security solutions, please refer to [Generate RTM Token](https://docs.agora.io/en/agora-class/agora_class_prep?platform=Android)。

   > See [Set up Authentication](https://docs.agora.io/en/Agora%20Platform/token) to learn how to get an App ID and access token. You can get a temporary access token to quickly try out this sample project.
   >
   > The Channel name you used to generate the token must be the same as the channel name you use to join a channel.

   > To ensure communication security, Agora uses access tokens (dynamic keys) to authenticate users joining a channel.
   >
   > Temporary access tokens are for demonstration and testing purposes only and remain valid for 24 hours. In a production environment, you need to deploy your own server for generating access tokens. See [Generate a Token](https://docs.agora.io/en/Interactive%20Broadcast/token_server) for details.

### Manually access the SDK
- Reference [Quick Access](https://docs.agora.io/en/agora-class/agora_class_quickstart_android?platform=Android)

## Feedback

If you have any problems or suggestions regarding the sample projects, feel free to file an [issue](https://github.com/AgoraIO-Community/CloudClass-Android/issues).

## Related resources

- Check our [FAQ](https://docs.agora.io/en/faq) to see if your issue has been recorded.
- Dive into [Agora SDK Samples](https://github.com/AgoraIO) to see more tutorials
- Take a look at [Agora Use Case](https://github.com/AgoraIO-usecase) for more complicated real use case
- Repositories managed by developer communities can be found at [Agora Community](https://github.com/AgoraIO-Community)
- If you encounter problems during integration, feel free to ask questions in [Stack Overflow](https://stackoverflow.com/questions/tagged/agora.io)

## License

The sample projects are under the MIT license.