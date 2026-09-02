#version 150

uniform sampler2D DiffuseSampler;
uniform float Slider;

in vec2 texCoord;

out vec4 fragColor;

void main(void)
{
    float OuterVig = 1.0f;

    float InnerVig = 1.0f;

    InnerVig = mix(1.0f, -1.0f, Slider);
    InnerVig = 0.0f;

    float blackOverlay = clamp(1.0f + InnerVig, 0.0f, 1.0f);

    vec2 uv = gl_FragCoord.xy;
    vec3 color = texture2D(DiffuseSampler, uv).rgb;
    vec2 center = vec2(0.5f,0.5f);

    float dist  = distance(center,uv )*1.414213f;

    float vig = clamp((OuterVig-dist) / (OuterVig-InnerVig), 0.0f, 1.0f);

    color *= vig;

    color *= blackOverlay;

    fragColor = vec4(color, 1.0);
}